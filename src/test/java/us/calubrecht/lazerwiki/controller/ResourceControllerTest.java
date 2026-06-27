package us.calubrecht.lazerwiki.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.service.MacroCssService;
import us.calubrecht.lazerwiki.service.ResourceService;
import us.calubrecht.lazerwiki.service.SiteService;

@WebMvcTest(controllers = {ResourceController.class, VersionController.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class ResourceControllerTest {
  @Autowired MockMvc mockMvc;

  @MockitoBean ResourceService resourceService;

  @MockitoBean MacroCssService cssService;

  @MockitoBean SiteService siteService;

  @Test
  void test_getFile() throws Exception {
    when(siteService.getSiteForHostname("localhost")).thenReturn("site1");
    this.mockMvc.perform(get("/_resources/someFile.jpg")).andExpect(status().isOk());

    verify(resourceService).getBinaryFile(eq("site1"), eq("someFile.jpg"));

    this.mockMvc.perform(get("/_resources/some.unknown_filetype")).andExpect(status().isOk());
    verify(resourceService).getBinaryFile(eq("site1"), eq("some.unknown_filetype"));

    when(resourceService.getBinaryFile(eq("site1"), eq("explosive.file")))
        .thenThrow(new IOException(""));
    this.mockMvc.perform(get("/_resources/explosive.file")).andExpect(status().isNotFound());
  }

  @Test
  void test_getFileInternal() throws Exception {
    when(cssService.getCss()).thenReturn("someCss");
    this.mockMvc
        .perform(get("/_resources/internal/plugin.css"))
        .andExpect(status().isOk())
        .andExpect(content().string("someCss"));

    this.mockMvc
        .perform(get("/_resources/internal/anything.else"))
        .andExpect(status().isNotFound());
  }
}
