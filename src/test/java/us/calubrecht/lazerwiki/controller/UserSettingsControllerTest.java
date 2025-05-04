package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.service.UserService;
import us.calubrecht.lazerwiki.service.exception.VerificationException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserSettingsController.class, VersionController.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class UserSettingsControllerTest {

    @Autowired
    UserSettingsController controller;

    @MockBean
    UserService userService;

    @Autowired
    MockMvc mockMvc;

    @Test
    void setPassword() throws Exception {
       when(userService.getUser("bob")).thenReturn(new User("Bob", null));
       mockMvc.perform(post("/api/users/setPassword").content("{\"userName\":\"Bob\", \"password\":\"OK\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("bob", ""))).
               andExpect(status().isOk()).andExpect(content().json("{\"success\":true, \"message\": \"\"}"));
       verify(userService).resetPassword("Bob", "OK");

       mockMvc.perform(post("/api/users/setPassword").content("{\"userName\":\"jim\", \"password\":\"OK\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("bob", ""))).
                andExpect(status().isUnauthorized());
    }

    @Test
    void saveEmail() throws Exception {
        when(userService.getUser("bob")).thenReturn(new User("Bob", null));
        mockMvc.perform(post("/api/users/saveEmail").content("{\"userName\":\"Bob\", \"email\":\"bob@super.com\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("bob", ""))).
                andExpect(status().isOk()).andExpect(content().json("{\"success\":true, \"message\": \"\"}"));
        verify(userService).requestSetEmail("Bob", "localhost", "bob@super.com");

        mockMvc.perform(post("/api/users/saveEmail").content("{\"userName\":\"jim\", \"email\":\"jimbo@super.com\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("bob", ""))).
                andExpect(status().isUnauthorized());
    }

    @Test
    void resetForgottenPassword() throws Exception {
        mockMvc.perform(post("/api/users/resetForgottenPassword").content("{\"userName\":\"Bob\", \"email\":\"bob@super.com\", \"password\":\"pass1\"}").contentType(MediaType.APPLICATION_JSON)).
                andExpect(status().isOk()).andExpect(content().json("{\"success\":true, \"message\": \"\"}"));
        verify(userService).requestResetForgottenPassword("Bob", "localhost", "bob@super.com", "pass1");
    }

    @Test
    void verifyEmailToken() throws Exception {
        when(userService.getUser("bob")).thenReturn(new User("Bob", null));
        mockMvc.perform(post("/api/users/verifyEmailToken").content("token1").principal(new UsernamePasswordAuthenticationToken("bob", ""))).
                andExpect(status().isOk()).andExpect(content().json("{\"success\":true, \"message\": \"\"}"));
        verify(userService).verifyEmailToken("Bob", "token1");

        doThrow(new VerificationException("Bad Token")).when(userService).verifyEmailToken("Bob", "token2");
        mockMvc.perform(post("/api/users/verifyEmailToken").content("token2").principal(new UsernamePasswordAuthenticationToken("bob", ""))).
                andExpect(status().isOk()).andExpect(content().json("{\"success\":false, \"message\": \"Bad Token\"}"));
    }

    @Test
    void verifyPasswordToken() throws Exception {
        mockMvc.perform(post("/api/users/verifyPasswordToken").content("{\"username\":\"Bob\", \"token\":\"token1\"}").contentType(MediaType.APPLICATION_JSON)).
                andExpect(status().isOk()).andExpect(content().json("{\"success\":true, \"message\": \"\"}"));
        verify(userService).verifyPasswordToken("Bob", "token1");

        doThrow(new VerificationException("Bad Token")).when(userService).verifyPasswordToken("Bob", "token2");
        mockMvc.perform(post("/api/users/verifyPasswordToken").content("{\"username\":\"Bob\", \"token\":\"token2\"}").contentType(MediaType.APPLICATION_JSON)).
                andExpect(status().isOk()).andExpect(content().json("{\"success\":false, \"message\": \"Bad Token\"}"));
    }
}
