package us.calubrecht.lazerwiki.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.model.UserRole;
import us.calubrecht.lazerwiki.repository.UserRepository;
import us.calubrecht.lazerwiki.util.PasswordUtil;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    public static final String SYS_USER="<SYS_USER>";

    @Autowired
    UserRepository userRepository;

    PasswordUtil passwordUtil = new PasswordUtil();

    @Transactional
    public void addUser(String userName, String password, List<GrantedAuthority> roles) throws NoSuchAlgorithmException, InvalidKeySpecException {

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

    public boolean verifyPassword(User u, String password) {
        return passwordUtil.matches(password, u.passwordHash);
    }
}
