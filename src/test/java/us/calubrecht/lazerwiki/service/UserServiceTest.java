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
import us.calubrecht.lazerwiki.repository.UserRepository;
import us.calubrecht.lazerwiki.util.PasswordUtil;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
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
    public void testCreateUser() throws NoSuchAlgorithmException, InvalidKeySpecException {
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
    public void testVerifyPassword() {
        when(userService.passwordUtil.matches(eq("ThisPass"), eq("cleverHash"))).thenReturn(true);

        User u = new User("Bob", "cleverHash");
        assertTrue(userService.verifyPassword(u, "ThisPass"));
        assertFalse(userService.verifyPassword(u, "NotThisPass"));
    }
}
