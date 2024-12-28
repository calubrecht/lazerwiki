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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
}
