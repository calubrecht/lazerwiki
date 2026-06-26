package us.calubrecht.lazerwiki.controller;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.service.SiteService;
import us.calubrecht.lazerwiki.service.UserService;
import us.calubrecht.lazerwiki.service.exception.RateLimitException;
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

    @MockitoBean
    UserService userService;

    @MockitoBean
    SiteService siteService;

    @Autowired
    MockMvc mockMvc;

    @Test
    void test_setPassword() throws Exception {
        User bob = new User("Bob", null);
        when(userService.getUser("bob")).thenReturn(bob);
        mockMvc
                .perform(
                        post("/api/users/setPassword")
                                .content("{\"userName\":\"Bob\", \"password\":\"OK\"}")
                                .contentType(MediaType.APPLICATION_JSON)
                                .principal(new UsernamePasswordAuthenticationToken("bob", "")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true, \"message\": \"\"}"));
        verify(userService).resetPassword("Bob", "OK", bob);

        mockMvc
                .perform(
                        post("/api/users/setPassword")
                                .content("{\"userName\":\"jim\", \"password\":\"OK\"}")
                                .contentType(MediaType.APPLICATION_JSON)
                                .principal(new UsernamePasswordAuthenticationToken("bob", "")))
                .andExpect(status().isForbidden());
    }

    @Test
    void test_saveEmail() throws Exception {
        when(userService.getUser("bob")).thenReturn(new User("Bob", null));
        mockMvc
                .perform(
                        post("/api/users/saveEmail")
                                .content("{\"userName\":\"Bob\", \"email\":\"bob@super.com\"}")
                                .contentType(MediaType.APPLICATION_JSON)
                                .principal(new UsernamePasswordAuthenticationToken("bob", "")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true, \"message\": \"\"}"));
        verify(userService).requestSetEmail("Bob", "localhost", "bob@super.com");


        mockMvc.perform(post("/api/users/saveEmail").content("{\"userName\":\"jim\", \"email\":\"jimbo@super.com\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("bob", ""))).
                andExpect(status().isForbidden());

        doThrow(new RateLimitException(60)).when(userService).requestSetEmail("Bob", "localhost", "bob@super.com");
        mockMvc.perform(post("/api/users/saveEmail").content("{\"userName\":\"Bob\", \"email\":\"bob@super.com\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("bob", ""))).
                andExpect(status().isTooManyRequests()).andExpect(content().json("{\"success\":false}"));
    }

    @Test
    void resetForgottenPassword() throws Exception {
        mockMvc.perform(post("/api/users/resetForgottenPassword").content("{\"userName\":\"Bob\", \"email\":\"bob@super.com\", \"password\":\"pass1\"}").contentType(MediaType.APPLICATION_JSON)).
                andExpect(status().isOk()).andExpect(content().json("{\"success\":true, \"message\": \"\"}"));
        verify(userService).requestResetForgottenPassword("Bob", "localhost", "bob@super.com", "pass1");

        doThrow(new RateLimitException(300)).when(userService).requestResetForgottenPassword("Bob", "localhost", "bob@super.com", "pass1");
        mockMvc.perform(post("/api/users/resetForgottenPassword").content("{\"userName\":\"Bob\", \"email\":\"bob@super.com\", \"password\":\"pass1\"}").contentType(MediaType.APPLICATION_JSON)).
                andExpect(status().isTooManyRequests()).andExpect(content().json("{\"success\":false}"));
    }

    @Test
    void test_verifyEmailToken() throws Exception {
        when(userService.getUser("bob")).thenReturn(new User("Bob", null));
        mockMvc
                .perform(
                        post("/api/users/verifyEmailToken")
                                .content("token1")
                                .principal(new UsernamePasswordAuthenticationToken("bob", "")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true, \"message\": \"\"}"));
        verify(userService).verifyEmailToken("Bob", "token1");

        doThrow(new VerificationException("Bad Token"))
                .when(userService)
                .verifyEmailToken("Bob", "token2");
        mockMvc
                .perform(
                        post("/api/users/verifyEmailToken")
                                .content("token2")
                                .principal(new UsernamePasswordAuthenticationToken("bob", "")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":false, \"message\": \"Bad Token\"}"));
    }

    @Test
    void test_verifyPasswordToken() throws Exception {
        mockMvc
                .perform(
                        post("/api/users/verifyPasswordToken")
                                .content("{\"username\":\"Bob\", \"token\":\"token1\"}")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true, \"message\": \"\"}"));
        verify(userService).verifyPasswordToken("Bob", "token1");

        doThrow(new VerificationException("Bad Token"))
                .when(userService)
                .verifyPasswordToken("Bob", "token2");
        mockMvc
                .perform(
                        post("/api/users/verifyPasswordToken")
                                .content("{\"username\":\"Bob\", \"token\":\"token2\"}")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":false, \"message\": \"Bad Token\"}"));
    }
}
