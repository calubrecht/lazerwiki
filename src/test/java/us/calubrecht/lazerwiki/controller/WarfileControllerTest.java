package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.service.MacroCssService;
import us.calubrecht.lazerwiki.service.ResourceService;
import us.calubrecht.lazerwiki.service.WarResourceService;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {WarfileController.class, VersionController.class})
@ActiveProfiles("test-sa")
@AutoConfigureMockMvc(addFilters = false)
class WarfileControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    WarResourceService resourceService;

    @Test
    void getFile() throws Exception {
        this.mockMvc.perform(get("/assets/someFile.jpg")).
                andExpect(status().isOk());

        verify(resourceService).getBinaryFile(eq("assets/someFile.jpg"));

        this.mockMvc.perform(get("/assets/some.unknown_filetype")).
                andExpect(status().isOk());
        verify(resourceService).getBinaryFile(eq("assets/some.unknown_filetype"));

        when(resourceService.getBinaryFile(eq("assets/explosive.file"))).thenThrow(
                new IOException(""));
        this.mockMvc.perform(get("/assets/explosive.file")).
                andExpect(status().isNotFound());
    }

    @Test
    void getIndexFile() throws Exception {
        when(resourceService.getBinaryFile("index.html")).thenReturn("Some bytes".getBytes());
        this.mockMvc.perform(get("/")).
                andExpect(status().isOk()).andExpect(content().string("Some bytes"));

        this.mockMvc.perform(get("")).
                andExpect(status().isOk()).andExpect(content().string("Some bytes"));

        this.mockMvc.perform(get("/page/theBigFile")).
                andExpect(status().isOk()).andExpect(content().string("Some bytes"));
    }

    @Test
    void getRootFile() throws Exception {
        when(resourceService.getBinaryFile("ding.txt")).thenReturn("Ding".getBytes());
        this.mockMvc.perform(get("/ding.txt")).
                andExpect(status().isOk()).andExpect(content().string("Ding"));
    }
}