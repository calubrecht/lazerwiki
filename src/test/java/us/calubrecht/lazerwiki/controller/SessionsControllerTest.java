package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    public void testUsername() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        this.mockMvc.perform(get("/api/sessions/username").principal(auth)).andExpect(status().isOk())
                .andExpect(content().string("Bob"));

    }

    @Test
    public void testLogout() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        MockHttpSession mockSession = new MockHttpSession();
        this.mockMvc.perform(post("/api/sessions/logout").principal(auth).session(mockSession)).andExpect(status().isOk());
        assertTrue(mockSession.isInvalid());
    }
}
