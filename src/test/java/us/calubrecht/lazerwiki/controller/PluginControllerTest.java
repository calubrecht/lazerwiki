package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.service.PluginService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PluginController.class, VersionController.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class PluginControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PluginService pluginService;

    @Test
    void getPluginJS() throws Exception {
        when(pluginService.getEditToolbarDefs(anyString())).thenReturn("ToolbarDefs");
        this.mockMvc.perform(get("/_resources/js/pluginJS.js")).
                andExpect(status().isOk()).andExpect(content().string("ToolbarDefs"));

    }
}