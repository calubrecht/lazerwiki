package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.model.UserRole;
import us.calubrecht.lazerwiki.service.SiteService;
import us.calubrecht.lazerwiki.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SessionsController.class, VersionController.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class SessionsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @MockBean
    SiteService siteService;

    @Test
    public void testUsername() throws Exception {
        User user = new User();
        user.roles = List.of(new UserRole(user, "ROLE_ADMIN"));
        when(userService.getUser(any())).thenReturn(user);
        when(siteService.getSiteForHostname("localhost")).thenReturn("default");
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        this.mockMvc.perform(get("/api/sessions/username").principal(auth)).andExpect(status().isOk())
                .andExpect(content().json("{\"userName\": \"Bob\", \"siteName\": \"default\", \"userRoles\":[\"ROLE_ADMIN\"]}"));
    }

    @Test
    public void testLogout() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        MockHttpSession mockSession = new MockHttpSession();
        this.mockMvc.perform(post("/api/sessions/logout").principal(auth).session(mockSession)).andExpect(status().isOk());
        assertTrue(mockSession.isInvalid());
    }
}
