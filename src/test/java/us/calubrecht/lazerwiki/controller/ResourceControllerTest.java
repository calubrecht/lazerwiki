package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.service.ResourceService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ResourceController.class, VersionController.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class ResourceControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    ResourceService resourceService;

    @Test
    void getFile() throws Exception {
        this.mockMvc.perform(get("/_resources/someFile.jpg")).
                andExpect(status().isOk());

        verify(resourceService).getBinaryFile(eq("localhost"), eq("someFile.jpg"));

        this.mockMvc.perform(get("/_resources/some.unknown_filetype")).
                andExpect(status().isOk());
        verify(resourceService).getBinaryFile(eq("localhost"), eq("some.unknown_filetype"));

        when(resourceService.getBinaryFile(eq("localhost"), eq("explosive.file"))).thenThrow(
                new IOException(""));
        this.mockMvc.perform(get("/_resources/explosive.file")).
                andExpect(status().isNotFound());
    }

}