package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.service.UserService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminUserController.class, VersionController.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class AdminUserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;


    @Test
    public void testCreateFromLocalhost() throws Exception {
        String requestJson = "{\"userName\": \"Bob\", \"password\": \"BigSecret\"}";
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        this.mockMvc.perform(post("/specialAdmin/createNewAdmin").
                with(request -> {request.setRemoteAddr("127.0.0.1"); return request;}).
                contentType(MediaType.APPLICATION_JSON).
                content(requestJson)).
                andExpect(status().isOk());

        verify(userService).addUser(eq("Bob"), eq("BigSecret"), isNull(), any());
    }

    @Test
    public void testCreateFromRemote() throws Exception {
        String requestJson = "{\"userName\": \"Bob\", \"password\": \"BigSecret\"}";
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        this.mockMvc.perform(post("/specialAdmin/createNewAdmin").
                        with(request -> {request.setRemoteAddr("192.168.1.1"); return request;}).
                        contentType(MediaType.APPLICATION_JSON).
                        content(requestJson)).
                        andExpect(status().isNotFound());

        verify(userService, never()).addUser(any(), any(), any(), any());
    }

    @Test
    public void testCreateFromForwarded() throws Exception {
        String requestJson = "{\"userName\": \"Bob\", \"password\": \"BigSecret\"}";
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        this.mockMvc.perform(post("/specialAdmin/createNewAdmin").
                        with(request -> {request.setRemoteAddr("127.0.0.1"); return request;}).
                        contentType(MediaType.APPLICATION_JSON).
                        content(requestJson).
                        header("X-Real-IP", "192.168.1.1")).
                        andExpect(status().isNotFound());

        verify(userService, never()).addUser(any(), any(), any(), any());
    }

    @Test
    public void testCreateFromWeirdness() throws Exception {
        String requestJson = "{\"userName\": \"Bob\", \"password\": \"BigSecret\"}";
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        this.mockMvc.perform(post("/specialAdmin/createNewAdmin").
                        with(request -> {request.setRemoteAddr("invalid.invalid"); return request;}).
                        contentType(MediaType.APPLICATION_JSON).
                        content(requestJson)).
                andExpect(status().isNotFound());

        verify(userService, never()).addUser(any(), any(), any(), any());
    }
}
