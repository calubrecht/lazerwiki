package us.calubrecht.lazerwiki.service;

import jakarta.mail.MessagingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.UserRepository;
import us.calubrecht.lazerwiki.repository.VerificationTokenRepository;
import us.calubrecht.lazerwiki.service.exception.VerificationException;
import us.calubrecht.lazerwiki.util.DbSupport;
import us.calubrecht.lazerwiki.util.PasswordUtil;

@Service
public class UserService {
    final Logger logger = LogManager.getLogger(getClass());

    public static final String SYS_USER = "<SYS_USER>";

    public static final String MISSING_USER = "<Deleted User>";

    @Autowired
    UserRepository userRepository;

    @Autowired
    EmailService emailService;

    @Autowired
    TemplateService templateService;

    @Autowired
    RandomService randomService;

    @Autowired
    VerificationTokenRepository tokenRepository;

    @Autowired
    ActivityLogService activityLogService;

    @Autowired
    EmailRateLimitService emailRateLimitService;

    PasswordUtil passwordUtil = new PasswordUtil();

    @Transactional
    @CacheEvict(
            value = {"UserService-getUsers", "UserService-getUser"},
            allEntries = true)
    public void addUser(
            String userName, String password, User createdBy, List<GrantedAuthority> roles) {

        User newUser = new User(userName, passwordUtil.hashPassword(password));
        newUser.roles =
                roles.stream()
                        .map(role -> new UserRole(newUser, role.getAuthority()))
                        .collect(Collectors.toList());
        newUser.setSettings(Map.of());
        userRepository.save(newUser);
        String msg = createdBy == null ? "Self-register: " + newUser.userName : newUser.userName;
        activityLogService.log(ActivityType.ACTIVITY_PROTO_CREATE_USER, null, createdBy, msg);
    }

    @Transactional(readOnly = true)
    @Cacheable("UserService-getUser")
    public User getUser(String userName) {
        if (userName.equals(SYS_USER)) {
            User u = new User();
            u.userName = SYS_USER;
            u.roles = List.of(new UserRole(u, "ROLE_ADMIN"));
            return u;
        }
        User u = userRepository.findByUserName(userName).orElse(null);
        if (u != null) {
            Hibernate.initialize(u.roles);
        }
        return u;
    }

    @Transactional(readOnly = true)
    @Cacheable("UserService-getUsers")
    public List<UserDTO> getUsers() {
        return DbSupport.toStream(userRepository.findAll())
                .map(
                        user ->
                                new UserDTO(
                                        user.userName,
                                        null,
                                        user.roles.stream().map(role -> role.role).toList(),
                                        user.getSettings()))
                .toList();
    }

    public boolean verifyPassword(User u, String password) {
        return passwordUtil.matches(password, u.passwordHash);
    }

    @Transactional
    @CacheEvict(
            value = {"UserService-getUser", "UserService-getUsers"},
            allEntries = true)
    public UserDTO deleteRole(String userName, String userRole, User adminUser) {
        Optional<User> u = userRepository.findByUserName(userName);
        activityLogService.log(
                ActivityType.ACTIVITY_PROTO_CHANGE_ROLES, null, adminUser, userName + " - " + userRole);
        return u.map(
                        user -> {
                            Optional<UserRole> ur =
                                    user.roles.stream().filter(role -> role.role.equals(userRole)).findFirst();
                            ur.ifPresent(role -> user.roles.remove(role));
                            userRepository.save(user);
                            return new UserDTO(
                                    userName,
                                    "",
                                    user.roles.stream().map(uo -> uo.role).toList(),
                                    user.getSettings());
                        })
                .orElse(null);
    }

    @Transactional
    @CacheEvict(
            value = {"UserService-getUser", "UserService-getUsers"},
            allEntries = true)
    public UserDTO addRole(String userName, String userRole, User adminUser) {
        Optional<User> u = userRepository.findByUserName(userName);
        activityLogService.log(
                ActivityType.ACTIVITY_PROTO_CHANGE_ROLES, null, adminUser, userName + " + " + userRole);
        return u.map(
                        user -> {
                            // List<UserRole> modifiedRoles = new ArrayList<>(user.roles.stream().filter(ur ->
                            // !ur.role.equals(userRole)).toList());
                            Optional<UserRole> ur =
                                    user.roles.stream().filter(role -> role.role.equals(userRole)).findFirst();
                            if (ur.isEmpty()) {
                                user.roles.add(new UserRole(user, userRole));
                                userRepository.save(user);
                            }
                            return new UserDTO(
                                    userName,
                                    "",
                                    user.roles.stream().map(uo -> uo.role).toList(),
                                    user.getSettings());
                        })
                .orElse(null);
    }

    @Transactional
    @CacheEvict(
            value = {"UserService-getUser", "UserService-getUsers"},
            allEntries = true)
    public void resetPassword(String userName, String password, User resetBy) {
        Optional<User> u = userRepository.findByUserName(userName);
        u.ifPresent(
                (user) -> {
                    if ((user.getRolesString().contains("ROLE_ADMIN")
                            || user.getRolesString().contains("ROLE_USERADMIN"))
                            && !user.userName.equals(resetBy.userName)) {
                        if (!resetBy.getRolesString().contains(("ROLE_ADMIN"))) {
                            // Only a ROLE_ADMIN can reset the password of an admin or useradmin
                            logger.error(
                                    "Unauthorized password reset attempt. USERADMIN:{} attempted to reset password of admin {}",
                                    userName,
                                    resetBy.userName);
                            throw new RuntimeException("Invalid password reset");
                        }
                    }
                    user.passwordHash = passwordUtil.hashPassword(password);
                    userRepository.save(user);
                });
    }

    @Transactional
    public void requestSetEmail(String userName, String siteName, String email) throws MessagingException {
        emailRateLimitService.checkSetEmailRateLimit(userName);
        String site = siteName;
        String randomKey = randomService.randomKey(8);
        tokenRepository.deleteExpired();
        Optional<User> u = userRepository.findByUserName(userName);
        if (u.isEmpty()) {
            return;
        }
        tokenRepository.save(new VerificationToken(u.get(), randomKey, VerificationToken.Purpose.VERIFY_EMAIL, email));
        String body = templateService.getVerifyEmailTemplate(site, email, userName, randomKey);
        emailService.sendEmail(email, userName, "Verify Email", body);
    }

    @Transactional
    public void requestResetForgottenPassword(String userName, String siteName, String email, String password) throws MessagingException {
        Optional<User> u = userRepository.findByUserName(userName);
        if (u.isEmpty() || !email.equals(u.get().getSettings().get("email"))) {
            return;
        }
        emailRateLimitService.checkPasswordResetRateLimit(email);
        String site = siteName;
        String randomKey = randomService.randomKey(8);
        tokenRepository.deleteExpired();
        String passwordHash = passwordUtil.hashPassword(password);
        tokenRepository.save(new VerificationToken(u.get(), randomKey, VerificationToken.Purpose.RESET_PASSWORD, passwordHash));
        String body = templateService.getVerifyEmailTemplate(site, email, userName, randomKey);
        emailService.sendEmail(email, userName, "Reset Forgotten Password", body);
    }


    @Transactional
    @CacheEvict(
            value = {"UserService-getUsers", "UserService-getUser"},
            allEntries = true)
    public void verifyEmailToken(String userName, String token) throws VerificationException {
        tokenRepository.deleteExpired();
        VerificationToken savedToken =
                tokenRepository.findByUserUserNameAndTokenAndPurpose(
                        userName, token, VerificationToken.Purpose.VERIFY_EMAIL);
        if (savedToken == null) {
            throw new VerificationException("Invalid token: Please check token and try again");
        }
        Optional<User> u = userRepository.findByUserName(userName);
        u.ifPresent(
                user -> {
                    user.getSettings().put("email", savedToken.getData());
                    userRepository.save(user);
                });
        tokenRepository.delete(savedToken);
    }

    @Transactional
    @CacheEvict(
            value = {"UserService-getUsers", "UserService-getUser"},
            allEntries = true)
    public void verifyPasswordToken(String userName, String token) throws VerificationException {
        tokenRepository.deleteExpired();
        VerificationToken savedToken =
                tokenRepository.findByUserUserNameAndTokenAndPurpose(
                        userName, token, VerificationToken.Purpose.RESET_PASSWORD);
        if (savedToken == null) {
            throw new VerificationException("Invalid token: Please check token and try again");
        }
        Optional<User> u = userRepository.findByUserName(userName);
        u.ifPresent(
                (user) -> {
                    user.passwordHash = savedToken.getData();
                    userRepository.save(user);
                });
        tokenRepository.delete(savedToken);
    }

    @Transactional
    @CacheEvict(
            value = {"UserService-getUser", "UserService-getUsers"},
            allEntries = true)
    public void deleteUser(String userName, User deletedBy) {
        userRepository.deleteByUserName(userName);
        activityLogService.log(ActivityType.ACTIVITY_PROTO_DELETE_USER, null, deletedBy, userName);
    }

    @Transactional
    @CacheEvict(
            value = {"UserService-getUser", "UserService-getUsers"},
            allEntries = true)
    public UserDTO setSiteRoles(String user, String site, List<String> newRoles, User adminUser) {
        Optional<User> u = userRepository.findByUserName(user);
        Optional<UserDTO> dto =
                u.map(
                        user1 -> {
                            List<UserRole> userRoles =
                                    new ArrayList<>(
                                            newRoles.stream().map(role -> new UserRole(user1, role)).toList());
                            user1.roles.stream()
                                    .filter(
                                            role -> {
                                                String[] parts = role.role.split(":");
                                                return parts.length != 3 || !parts[1].equals(site);
                                            })
                                    .forEach(role -> userRoles.add(role));
                            user1.roles.clear();
                            user1.roles.addAll(userRoles);
                            userRepository.save(user1);
                            return new UserDTO(
                                    user,
                                    site,
                                    userRoles.stream().map(role -> role.role).toList(),
                                    user1.getSettings());
                        });
        activityLogService.log(
                ActivityType.ACTIVITY_PROTO_CHANGE_ROLES, null, adminUser, user + " set Multiple Roles");
        return dto.orElseGet(null);
    }
}
