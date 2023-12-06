package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.service.PageService;
import us.calubrecht.lazerwiki.service.PluginService;

import static org.junit.jupiter.api.Assertions.*;
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

    @MockBean
    PluginService pluginService;

    @Test
    void getPluginJS() throws Exception {
        when(pluginService.getEditToolbarDefs()).thenReturn("ToolbarDefs");
        this.mockMvc.perform(get("/_resources/js/pluginJS.js")).
                andExpect(status().isOk()).andExpect(content().string("ToolbarDefs"));

    }
}