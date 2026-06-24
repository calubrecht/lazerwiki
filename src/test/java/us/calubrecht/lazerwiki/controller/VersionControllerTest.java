package us.calubrecht.lazerwiki.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = VersionController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class VersionControllerTest {

  @Autowired MockMvc mockMvc;

  @Test
  public void test_version() throws Exception {
    this.mockMvc
        .perform(get("/api/version"))
        .andExpect(status().isOk())
        .andExpect(content().string("test.version"));

    assertEquals("test.version", VersionController.getVersion());
  }
}
