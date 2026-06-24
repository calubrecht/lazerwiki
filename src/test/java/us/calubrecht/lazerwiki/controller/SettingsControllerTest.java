package us.calubrecht.lazerwiki.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.model.GlobalSettings;
import us.calubrecht.lazerwiki.service.GlobalSettingsService;

@WebMvcTest(controllers = SettingsController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class SettingsControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean GlobalSettingsService globalSettingsService;

  @Test
  void getGlobalSettings() throws Exception {
    GlobalSettings settings = new GlobalSettings();
    settings.settings = Map.of("Setting1", "value1");
    when(globalSettingsService.getSettings()).thenReturn(settings);

    this.mockMvc
        .perform(get("/api/settings/globalSettings"))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"settings\":{\"Setting1\":\"value1\"}}"));
  }
}
