package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.service.SiteService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SiteController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class SiteControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    SiteService siteService;

    @Test
    public void testVersion() throws Exception {
        when(siteService.getSiteNameForHostname(("localhost"))).thenReturn("This wiki");
        this.mockMvc.perform(get("/api/site/")).andExpect(status().isOk())
                .andExpect(content().string("This wiki"));

    }

}