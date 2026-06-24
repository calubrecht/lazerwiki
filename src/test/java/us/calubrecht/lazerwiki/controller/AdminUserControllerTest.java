package us.calubrecht.lazerwiki.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.service.UserService;

@WebMvcTest(controllers = {AdminUserController.class, VersionController.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class AdminUserControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean UserService userService;

  @Test
  public void test_createFromLocalhost() throws Exception {
    String requestJson = "{\"userName\": \"Bob\", \"password\": \"BigSecret\"}";
    this.mockMvc
        .perform(
            post("/specialAdmin/createNewAdmin")
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      return request;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
        .andExpect(status().isOk());

    verify(userService).addUser(eq("Bob"), eq("BigSecret"), isNull(), any());
  }

  @Test
  public void test_createFromRemote() throws Exception {
    String requestJson = "{\"userName\": \"Bob\", \"password\": \"BigSecret\"}";
    this.mockMvc
        .perform(
            post("/specialAdmin/createNewAdmin")
                .with(
                    request -> {
                      request.setRemoteAddr("192.168.1.1");
                      return request;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
        .andExpect(status().isNotFound());

    verify(userService, never()).addUser(any(), any(), any(), any());
  }

  @Test
  public void test_createFromForwarded() throws Exception {
    String requestJson = "{\"userName\": \"Bob\", \"password\": \"BigSecret\"}";
    this.mockMvc
        .perform(
            post("/specialAdmin/createNewAdmin")
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      return request;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .header("X-Real-IP", "192.168.1.1"))
        .andExpect(status().isNotFound());

    verify(userService, never()).addUser(any(), any(), any(), any());
  }

  @Test
  public void test_createFromWeirdness() throws Exception {
    String requestJson = "{\"userName\": \"Bob\", \"password\": \"BigSecret\"}";
    this.mockMvc
        .perform(
            post("/specialAdmin/createNewAdmin")
                .with(
                    request -> {
                      request.setRemoteAddr("invalid.invalid");
                      return request;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
        .andExpect(status().isNotFound());

    verify(userService, never()).addUser(any(), any(), any(), any());
  }
}
