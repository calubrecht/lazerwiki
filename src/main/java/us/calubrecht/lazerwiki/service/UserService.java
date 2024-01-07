package us.calubrecht.lazerwiki.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.model.UserDTO;
import us.calubrecht.lazerwiki.model.UserRole;
import us.calubrecht.lazerwiki.repository.UserRepository;
import us.calubrecht.lazerwiki.util.PasswordUtil;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UserService {

    public static final String SYS_USER="<SYS_USER>";

    @Autowired
    UserRepository userRepository;

    PasswordUtil passwordUtil = new PasswordUtil();

    @Transactional
    public void addUser(String userName, String password, List<GrantedAuthority> roles) {

        User newUser = new User(userName, passwordUtil.hashPassword(password));
        newUser.roles = roles.stream().map(role -> new UserRole(newUser, role.getAuthority())).collect(Collectors.toList());
        userRepository.save(newUser);
    }

    @Transactional
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

    public List<UserDTO> getUsers() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false).
                map(user -> new UserDTO(user.userName, null, user.roles.stream().map(role -> role.role).toList()
                )).toList();
    }

    public boolean verifyPassword(User u, String password) {
        return passwordUtil.matches(password, u.passwordHash);
    }

    @Transactional
    public UserDTO deleteRole(String userName, String userRole) {
        Optional<User> u = userRepository.findById(userName);
        return u.map(user -> {
            Optional<UserRole> ur = user.roles.stream().filter(role -> role.role.equals(userRole)).findFirst();
            ur.ifPresent( role -> user.roles.remove(role));
            userRepository.save(user);
            return new UserDTO(userName, "", user.roles.stream().map(uo-> uo.role).toList());
        }).orElse(null);
    }

    @Transactional
    public UserDTO addRole(String userName, String userRole) {
        Optional<User> u = userRepository.findById(userName);
        return u.map(user -> {
            // List<UserRole> modifiedRoles = new ArrayList<>(user.roles.stream().filter(ur -> !ur.role.equals(userRole)).toList());
            Optional<UserRole> ur = user.roles.stream().filter(role -> role.role.equals(userRole)).findFirst();
            if (!ur.isPresent() ) {
                user.roles.add(new UserRole(user, userRole));
                userRepository.save(user);
            }
            return new UserDTO(userName, "", user.roles.stream().map(uo-> uo.role).toList());
        }).orElse(null);
    }
}
