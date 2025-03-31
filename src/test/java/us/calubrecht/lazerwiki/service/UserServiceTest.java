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
import us.calubrecht.lazerwiki.repository.VerificationTokenRepository;
import us.calubrecht.lazerwiki.util.PasswordUtil;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = UserService.class)
@ActiveProfiles("test")
public class UserServiceTest {

    @Autowired
    UserService userService;

    @MockBean
    UserRepository userRepository;

    @MockBean
    SiteService siteService;

    @MockBean
    VerificationTokenRepository tokenRepository;

    @MockBean
    EmailService emailService;

    @MockBean
    RandomService randomService;

    @MockBean
    TemplateService templateService;

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
        assertEquals(Map.of(), u.settings);
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
        users.get(0).settings = Map.of();
        users.get(1).settings = Map.of();
        when(userRepository.findAll()).thenReturn(users);

        assertEquals(List.of(
                new UserDTO("bob", null, List.of("ROLE_ADMIN"), Map.of()),
                new UserDTO("Joe", null, Collections.emptyList(), Map.of())), userService.getUsers());
    }

    @Test
    public void testDeleteRoles() {
        User u = new User("Frank", "pass");
        u.roles = new ArrayList<>(List.of(new UserRole(u, "ROLE_ADMIN"), new UserRole(u, "ROLE_USER")));
        when(userRepository.findById("Frank")).thenReturn(Optional.of(u));
        UserDTO changedUser = userService.deleteRole("Frank", "ROLE_ADMIN");

        assertEquals(List.of("ROLE_USER"), changedUser.userRoles());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(List.of("ROLE_USER"), captor.getValue().roles.stream().map(ur -> ur.role).toList());
    }

    @Test
    public void testAddRoles() {
        User u = new User("Frank", "pass");
        u.roles = new ArrayList<>(List.of(new UserRole(u, "ROLE_ADMIN"), new UserRole(u, "ROLE_USER")));
        when(userRepository.findById("Frank")).thenReturn(Optional.of(u));
        UserDTO changedUser = userService.addRole("Frank", "ROLE_ADMIN:oneSite");

        assertEquals(List.of("ROLE_ADMIN", "ROLE_USER", "ROLE_ADMIN:oneSite"), changedUser.userRoles());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(List.of("ROLE_ADMIN", "ROLE_USER", "ROLE_ADMIN:oneSite"), captor.getValue().roles.stream().map(ur -> ur.role).toList());

        changedUser = userService.addRole("Frank", "ROLE_ADMIN:oneSite");
        assertEquals(List.of("ROLE_ADMIN", "ROLE_USER", "ROLE_ADMIN:oneSite"), changedUser.userRoles());
        // Don't save again if role already exists'
        verify(userRepository, times(1)).save(captor.capture());

    }

    @Test
    void testResetPassword() {
        User u = new User("Frank", "pass");
        when(userService.passwordUtil.hashPassword(eq("password"))).thenReturn("hash");
        when(userRepository.findById("Frank")).thenReturn(Optional.of(u));

        userService.resetPassword("Frank", "password");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("hash", captor.getValue().passwordHash);

        // Reset non-existant password is noop
        userService.resetPassword("NotThere", "password");
        // Only called once above
        verify(userRepository, times(1)).save(any());
    }

    @Test
    public void test_deleteUser() {
        userService.deleteUser("Frank");
        verify(userRepository).deleteById("Frank");
    }
}
