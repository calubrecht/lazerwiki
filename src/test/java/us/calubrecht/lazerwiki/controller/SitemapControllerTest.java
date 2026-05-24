package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.service.SitemapService;

import java.net.URL;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SitemapController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class SitemapControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SitemapService sitemapService;

    @Test
    public void testSitemap() throws Exception {
        when(sitemapService.getSitemap(new URL("http://localhost/sitemap.xml"))).thenReturn("<xml>Wow</xml>");
        this.mockMvc.perform(get("/sitemap.xml")).andExpect(status().isOk())
                .andExpect(content().string("<xml>Wow</xml>"));

    }
}
