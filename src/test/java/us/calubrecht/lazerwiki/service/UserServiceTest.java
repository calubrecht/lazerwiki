package us.calubrecht.lazerwiki.service;

import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.LazerWikiAuthenticationManager;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.UserRepository;
import us.calubrecht.lazerwiki.repository.VerificationTokenRepository;
import us.calubrecht.lazerwiki.service.exception.VerificationException;
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

    @MockBean
    ActivityLogService activityLogService;

    @BeforeEach
    public void setup() {
        userService.passwordUtil = Mockito.mock(PasswordUtil.class);
    }

    @Test
    public void testCreateUser() {
        when(userService.passwordUtil.hashPassword(eq("password"))).thenReturn("hash");
        User createdBy = new User("Admin", "");
        userService.addUser("Bob", "password", createdBy, List.of(LazerWikiAuthenticationManager.USER));
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

        verify(activityLogService).log(eq(ActivityType.ACTIVITY_PROTO_CREATE_USER), isNull(), eq(createdBy), eq("Bob"));

        userService.addUser("Jake", "password", null, List.of(LazerWikiAuthenticationManager.USER));

        verify(activityLogService).log(eq(ActivityType.ACTIVITY_PROTO_CREATE_USER), isNull(), isNull(), eq("Self-register: Jake"));
    }

    @Test
    public void testGetUser() {
        User u = new User("Bob", "pass");
        u.roles = Collections.emptyList();
        when(userRepository.findByUserName("realUser")).thenReturn(Optional.of(u));

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
        when(userRepository.findByUserName("Frank")).thenReturn(Optional.of(u));
        UserDTO changedUser = userService.deleteRole("Frank", "ROLE_ADMIN", null);

        assertEquals(List.of("ROLE_USER"), changedUser.userRoles());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(List.of("ROLE_USER"), captor.getValue().roles.stream().map(ur -> ur.role).toList());
    }

    @Test
    public void testAddRoles() {
        User u = new User("Frank", "pass");
        u.roles = new ArrayList<>(List.of(new UserRole(u, "ROLE_ADMIN"), new UserRole(u, "ROLE_USER")));
        when(userRepository.findByUserName("Frank")).thenReturn(Optional.of(u));
        UserDTO changedUser = userService.addRole("Frank", "ROLE_ADMIN:oneSite", null);

        assertEquals(List.of("ROLE_ADMIN", "ROLE_USER", "ROLE_ADMIN:oneSite"), changedUser.userRoles());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(List.of("ROLE_ADMIN", "ROLE_USER", "ROLE_ADMIN:oneSite"), captor.getValue().roles.stream().map(ur -> ur.role).toList());

        changedUser = userService.addRole("Frank", "ROLE_ADMIN:oneSite", null);
        assertEquals(List.of("ROLE_ADMIN", "ROLE_USER", "ROLE_ADMIN:oneSite"), changedUser.userRoles());
        // Don't save again if role already exists'
        verify(userRepository, times(1)).save(captor.capture());

    }

    @Test
    void testResetPassword() {
        User u = new User("Frank", "pass");
        when(userService.passwordUtil.hashPassword(eq("password"))).thenReturn("hash");
        when(userRepository.findByUserName("Frank")).thenReturn(Optional.of(u));

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
        User user = new User ("Bob", "hash");
        userService.deleteUser("Frank", user);
        verify(userRepository).deleteByUserName("Frank");
        verify(activityLogService).log(eq(ActivityType.ACTIVITY_PROTO_DELETE_USER), isNull(), eq(user), eq("Frank"));
    }

    @Test
    void requestSetEmail() throws MessagingException {
        when(siteService.getSiteNameForHostname("localhost")).thenReturn("Lazerwiki");
        when(randomService.randomKey(8)).thenReturn("ABCD-EFGH");
        when(templateService.getVerifyEmailTemplate(eq("Lazerwiki"), eq("bob@super.com"), eq("Bob"), eq( "ABCD-EFGH"))).thenReturn("The template");
        User user = new User ("Bob", "hash");
        when(userRepository.findByUserName("Bob")).thenReturn(Optional.of(user));
        userService.requestSetEmail("Bob", "localhost", "bob@super.com");

        verify(tokenRepository).deleteExpired();
        verify(tokenRepository).save(new VerificationToken(user, "ABCD-EFGH", VerificationToken.Purpose.VERIFY_EMAIL, "bob@super.com"));
        verify(emailService).sendEmail(eq("localhost"), eq("bob@super.com"), eq("Bob"), eq("Verify Email"), eq("The template"));

        Mockito.reset(tokenRepository);
        Mockito.reset(emailService);
        userService.requestSetEmail("Jeff", "localhost", "bob@super.com");
        verify(tokenRepository, Mockito.never()).save(any());
        verify(emailService, Mockito.never()).sendEmail(any(), any(), any(), any(), any());
    }

    @Test
    void requestResetForgottenPassword() throws MessagingException {
        when(siteService.getSiteNameForHostname("localhost")).thenReturn("Lazerwiki");
        when(randomService.randomKey(8)).thenReturn("ABCD-EFGH");
        when(templateService.getVerifyEmailTemplate(eq("Lazerwiki"), eq("bob@super.com"), eq("Bob"), eq( "ABCD-EFGH"))).thenReturn("The template");
        when(userService.passwordUtil.hashPassword(eq("pass"))).thenReturn("newHash");
        User user = new User("Bob", "hash");
        user.setSettings(Map.of("email", "bob@super.com"));
        when(userRepository.findByUserName("Bob")).thenReturn(Optional.of(user));
        userService.requestResetForgottenPassword("Bob", "localhost", "bob@super.com", "pass");

        verify(tokenRepository).deleteExpired();
        verify(tokenRepository).save(new VerificationToken(user, "ABCD-EFGH", VerificationToken.Purpose.RESET_PASSWORD, "newHash"));
        verify(emailService).sendEmail(eq("localhost"), eq("bob@super.com"), eq("Bob"), eq("Reset Forgotten Password"), eq("The template"));

        userService.requestResetForgottenPassword("Bob", "localhost", "bob@super.com", "pass");

        Mockito.reset(tokenRepository);
        Mockito.reset(emailService);
        userService.requestResetForgottenPassword("Bob", "localhost", "joe@super.com", "pass");
        verify(tokenRepository, Mockito.never()).save(any());
        verify(emailService, Mockito.never()).sendEmail(any(), any(), any(), any(), any());

        userService.requestResetForgottenPassword("Jeff", "localhost", "bob@super.com", "pass");
        verify(tokenRepository, Mockito.never()).save(any());
        verify(emailService, Mockito.never()).sendEmail(any(), any(), any(), any(), any());
    }

    @Test
    void verifyEmailToken() throws VerificationException {
        User user = new User("Bob", "hash");
        user.setSettings(new HashMap<>());
        when(tokenRepository.findByUserUserNameAndTokenAndPurpose(eq("Bob"), eq("ABCD-EFGH"), eq(VerificationToken.Purpose.VERIFY_EMAIL))).thenReturn(
                new VerificationToken(user, "ABCD-EFGH", VerificationToken.Purpose.VERIFY_EMAIL, "bob@super.com"));
        when(userRepository.findByUserName("Bob")).thenReturn(Optional.of(user));
        userService.verifyEmailToken("Bob", "ABCD-EFGH");

        User updateUser = new User("Bob", "hash");
        updateUser.setSettings(Map.of("email", "bob@super.com"));
        verify(userRepository).save(updateUser);

        assertThrows(VerificationException.class, () ->  userService.verifyEmailToken("Bob", "ABCD-WXYZ"));
    }

    @Test
    void verifyPasswordToken() throws VerificationException {
        User user = new User("Bob", "hash");
        when(tokenRepository.findByUserUserNameAndTokenAndPurpose(eq("Bob"), eq("ABCD-EFGH"), eq(VerificationToken.Purpose.RESET_PASSWORD))).thenReturn(
                new VerificationToken(user, "ABCD-EFGH", VerificationToken.Purpose.RESET_PASSWORD, "newPasswordHash"));
        when(userRepository.findByUserName("Bob")).thenReturn(Optional.of(user));
        userService.verifyPasswordToken("Bob", "ABCD-EFGH");

        User updateUser = new User("Bob", "newPasswordHash");
        verify(userRepository).save(updateUser);

        assertThrows(VerificationException.class, () ->  userService.verifyPasswordToken("Bob", "ABCD-WXYZ"));
    }

    @Test
    void setSiteRoles() {
        User user = new User("Bob", "hash");
        user.roles = new ArrayList<>(List.of("ROLE_READ:site1:ns2", "ROLE_ADMIN", "ROLE_WRITE:site2:ns3").stream().
                map(roleName -> new UserRole(user, roleName)).toList());
        when(userRepository.findByUserName("Bob")).thenReturn(Optional.of(user));
        UserDTO dto = userService.setSiteRoles("Bob", "site1", List.of("ROLE_READ:site1:ns1", "ROLE_WRITE:site1:ns2"), null);

        User savedUser = new User("Bob", "hash");
        savedUser.roles = List.of("ROLE_READ:site1:ns1", "ROLE_WRITE:site1:ns2", "ROLE_ADMIN", "ROLE_WRITE:site2:ns3").stream().
                map(roleName -> new UserRole(user, roleName)).toList();
        verify(userRepository).save(savedUser);
        assertEquals(List.of("ROLE_READ:site1:ns1", "ROLE_WRITE:site1:ns2", "ROLE_ADMIN", "ROLE_WRITE:site2:ns3"), dto.userRoles());
    }
}
