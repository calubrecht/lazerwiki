package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.LazerWikiAuthenticationManager;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.model.UserDTO;
import us.calubrecht.lazerwiki.model.UserRole;
import us.calubrecht.lazerwiki.repository.UserRepository;
import us.calubrecht.lazerwiki.util.PasswordUtil;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = UserService.class)
@ActiveProfiles("test")
public class UserServiceTest {

    @Autowired
    UserService userService;

    @MockBean
    UserRepository userRepository;

    @BeforeEach
    public void setup() {
        userService.passwordUtil = Mockito.mock(PasswordUtil.class);
    }

    @Test
    public void testCreateUser() {
        when(userService.passwordUtil.hashPassword(eq("password"))).thenReturn("hash");
        userService.addUser("Bob", "password",  List.of(LazerWikiAuthenticationManager.USER));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertEquals(1, userCaptor.getAllValues().size());
        User u = userCaptor.getValue();

        assertEquals("Bob", u.userName);
        assertNotNull(u.passwordHash);
        assertNotEquals("password", u.passwordHash);
        assertEquals(1, u.roles.size());
        assertEquals("ROLE_USER", u.roles.get(0).role);
    }

    @Test
    public void testGetUser() {
        User u = new User("Bob", "pass");
        u.roles = Collections.emptyList();
        when(userRepository.findById("realUser")).thenReturn(Optional.of(u));

        assertEquals("Bob", userService.getUser("realUser").userName);
        assertEquals(null, userService.getUser("MissingUser"));
    }

    @Test
    public void testGetSysUser() {
        assertEquals("ROLE_ADMIN", userService.getUser(UserService.SYS_USER).roles.get(0).role);
    }

    @Test
    public void testVerifyPassword() {
        when(userService.passwordUtil.matches(eq("ThisPass"), eq("cleverHash"))).thenReturn(true);

        User u = new User("Bob", "cleverHash");
        assertTrue(userService.verifyPassword(u, "ThisPass"));
        assertFalse(userService.verifyPassword(u, "NotThisPass"));
    }

    @Test
    public void testGetUsers() {
        List<User> users = List.of (new User("bob",""), new User("Joe", ""));
        users.get(0).roles = List.of(new UserRole(users.get(0), "ROLE_ADMIN"));
        users.get(1).roles = Collections.emptyList();
        when(userRepository.findAll()).thenReturn(users);

        assertEquals(List.of(
                new UserDTO("bob", null, List.of("ROLE_ADMIN")),
                new UserDTO("Joe", null, Collections.emptyList())), userService.getUsers());
    }

    @Test
    public void testDeleteRoles() {
        User u = new User("Frank", "pass");
        u.roles = List.of(new UserRole(u, "ROLE_ADMIN"), new UserRole(u, "ROLE_USER"));
        when(userRepository.findById("Frank")).thenReturn(Optional.of(u));
        UserDTO changedUser = userService.deleteRole("Frank", "ROLE_ADMIN");

        assertEquals(List.of("ROLE_USER"), changedUser.userRoles());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(List.of("ROLE_USER"), captor.getValue().roles.stream().map(ur -> ur.role).toList());
    }
}
