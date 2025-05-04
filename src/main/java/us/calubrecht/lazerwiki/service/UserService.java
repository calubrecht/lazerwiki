package us.calubrecht.lazerwiki.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.model.UserDTO;
import us.calubrecht.lazerwiki.model.UserRole;
import us.calubrecht.lazerwiki.model.VerificationToken;
import us.calubrecht.lazerwiki.repository.UserRepository;
import us.calubrecht.lazerwiki.repository.VerificationTokenRepository;
import us.calubrecht.lazerwiki.service.exception.VerificationException;
import us.calubrecht.lazerwiki.util.DbSupport;
import us.calubrecht.lazerwiki.util.PasswordUtil;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UserService {

    public static final String SYS_USER="<SYS_USER>";

    @Autowired
    UserRepository userRepository;

    @Autowired
    SiteService siteService;

    @Autowired
    EmailService emailService;

    @Autowired
    TemplateService templateService;

    @Autowired
    RandomService randomService;

    @Autowired
    VerificationTokenRepository tokenRepository;

    PasswordUtil passwordUtil = new PasswordUtil();

    @Transactional
    @CacheEvict(value = {"UserService-getUsers", "UserService-getUser"}, allEntries = true)
    public void addUser(String userName, String password, List<GrantedAuthority> roles) {

        User newUser = new User(userName, passwordUtil.hashPassword(password));
        newUser.roles = roles.stream().map(role -> new UserRole(newUser, role.getAuthority())).collect(Collectors.toList());
        newUser.settings = Map.of();
        userRepository.save(newUser);
    }

    @Transactional
    @Cacheable("UserService-getUser")
    public User getUser(String userName) {
        if (userName.equals(SYS_USER)) {
            User u = new User();
            u.userName = SYS_USER;
            u.roles = List.of(new UserRole(u, "ROLE_ADMIN"));
            return u;
        }
        User u = userRepository.findById(userName).orElse(null);
        if (u != null) {
            u.roles.size();
        }
        return u;
    }

    @Cacheable("UserService-getUsers")
    public List<UserDTO> getUsers() {
        return DbSupport.toStream(userRepository.findAll()).
                map(user -> new UserDTO(user.userName, null, user.roles.stream().map(role -> role.role).toList(), user.getSettings()
                )).toList();
    }

    public boolean verifyPassword(User u, String password) {
        return passwordUtil.matches(password, u.passwordHash);
    }

    @Transactional
    @CacheEvict(value = {"UserService-getUser", "UserService-getUsers"}, allEntries = true)
    public UserDTO deleteRole(String userName, String userRole) {
        Optional<User> u = userRepository.findById(userName);
        return u.map(user -> {
            Optional<UserRole> ur = user.roles.stream().filter(role -> role.role.equals(userRole)).findFirst();
            ur.ifPresent( role -> user.roles.remove(role));
            userRepository.save(user);
            return new UserDTO(userName, "", user.roles.stream().map(uo-> uo.role).toList(), user.getSettings());
        }).orElse(null);
    }

    @Transactional
    @CacheEvict(value = {"UserService-getUser", "UserService-getUsers"}, allEntries = true)
    public UserDTO addRole(String userName, String userRole) {
        Optional<User> u = userRepository.findById(userName);
        return u.map(user -> {
            // List<UserRole> modifiedRoles = new ArrayList<>(user.roles.stream().filter(ur -> !ur.role.equals(userRole)).toList());
            Optional<UserRole> ur = user.roles.stream().filter(role -> role.role.equals(userRole)).findFirst();
            if (!ur.isPresent() ) {
                user.roles.add(new UserRole(user, userRole));
                userRepository.save(user);
            }
            return new UserDTO(userName, "", user.roles.stream().map(uo-> uo.role).toList(), user.getSettings());
        }).orElse(null);
    }

    @Transactional
    @CacheEvict(value = {"UserService-getUser", "UserService-getUsers"}, allEntries = true)
    public void resetPassword(String userName, String password) {
        Optional<User> u = userRepository.findById(userName);
        u.ifPresent((user) -> {
            user.passwordHash = passwordUtil.hashPassword(password);
            userRepository.save(user);
        });
    }

    @Transactional
    public void requestSetEmail(String userName, String host, String email) throws MessagingException {
        String site = siteService.getSiteNameForHostname(host);
        String randomKey = randomService.randomKey(8);
        tokenRepository.deleteExpired();
        tokenRepository.save(new VerificationToken(userName, randomKey, VerificationToken.Purpose.VERIFY_EMAIL, email));
        String body = templateService.getVerifyEmailTemplate(site, email, userName, randomKey);
        emailService.sendEmail(host, email, userName,"Verify Email", body);
    }

    @Transactional
    public void requestResetForgottenPassword(String userName, String host, String email, String password) throws MessagingException {
        Optional<User> u = userRepository.findById(userName);
        if (!u.isPresent() || !email.equals(u.get().getSettings().get("email"))) {
            return;
        }
        String site = siteService.getSiteNameForHostname(host);
        String randomKey = randomService.randomKey(8);
        tokenRepository.deleteExpired();
        String passwordHash = passwordUtil.hashPassword(password);
        tokenRepository.save(new VerificationToken(userName, randomKey, VerificationToken.Purpose.RESET_PASSWORD, passwordHash));
        String body = templateService.getVerifyEmailTemplate(site, email, userName, randomKey);
        emailService.sendEmail(host, email, userName,"Reset Forgotten Password", body);
    }

    @Transactional
    @CacheEvict(value = {"UserService-getUsers", "UserService-getUser"}, allEntries = true)
    public void verifyEmailToken(String userName, String token) throws VerificationException {
        VerificationToken savedToken = tokenRepository.findByUserAndTokenAndPurpose(userName, token, VerificationToken.Purpose.VERIFY_EMAIL);
        if (savedToken == null) {
            throw new VerificationException("Invalid token: Please check token and try again");
        }
        Optional<User> u = userRepository.findById(userName);
        u.get().getSettings().put("email", savedToken.getData());
        userRepository.save(u.get());
    }

    @Transactional
    @CacheEvict(value = {"UserService-getUsers", "UserService-getUser"}, allEntries = true)
    public void verifyPasswordToken(String userName, String token) throws VerificationException {
        VerificationToken savedToken = tokenRepository.findByUserAndTokenAndPurpose(userName, token, VerificationToken.Purpose.RESET_PASSWORD);
        if (savedToken == null) {
            throw new VerificationException("Invalid token: Please check token and try again");
        }
        Optional<User> u = userRepository.findById(userName);
        u.ifPresent((user) -> {
            user.passwordHash = savedToken.getData();
            userRepository.save(user);
        });
    }

    @Transactional
    @CacheEvict(value = {"UserService-getUser", "UserService-getUsers"}, allEntries = true)
    public void deleteUser(String userName) {
       userRepository.deleteById(userName);
    }

    @Transactional
    @CacheEvict(value = {"UserService-getUser", "UserService-getUsers"}, allEntries = true)
    public UserDTO setSiteRoles(String user, String site, List<String> newRoles) {
        Optional<User> u = userRepository.findById(user);
        Optional<UserDTO> dto = u.map(user1 -> {
            List<UserRole> userRoles = new ArrayList<>(newRoles.stream().map(role -> new UserRole(user1, role)).toList());
            user1.roles.stream().filter(role -> {
                String[] parts = role.role.split(":");
                return parts.length !=3 || !parts[1].equals(site);}).forEach(role ->
                    userRoles.add(role));
            user1.roles.clear();
            user1.roles.addAll(userRoles);
           userRepository.save(user1);
           return new UserDTO (user, site, userRoles.stream().map(role -> role.role).toList(), user1.getSettings());
        });
        return dto.orElseGet(null);
    }
}
