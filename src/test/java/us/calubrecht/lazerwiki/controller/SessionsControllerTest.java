package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@WebMvcTest(controllers = SessionsController.class)
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
}
