package us.calubrecht.lazerwiki.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.service.SiteService;

@WebMvcTest(controllers = SiteController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class SiteControllerTest {
  @Autowired MockMvc mockMvc;

  @MockitoBean SiteService siteService;

  @Test
  public void test_version() throws Exception {
    when(siteService.getSiteNameForHostname(("localhost"))).thenReturn("This wiki");
    this.mockMvc
        .perform(get("/api/site/"))
        .andExpect(status().isOk())
        .andExpect(content().string("This wiki"));
  }
}
