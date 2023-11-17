package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.service.MediaService;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {MediaController.class, VersionController.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class MediaControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    MediaService mediaService;

    @Test
    void getFile() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        this.mockMvc.perform(get("/_media/someFile.jpg").
                        principal(auth)).
                andExpect(status().isOk());

        verify(mediaService).getBinaryFile(eq("localhost"), eq("Bob"), eq("someFile.jpg"));

        this.mockMvc.perform(get("/_media/some.unknown_filetype")).
                andExpect(status().isOk());
        verify(mediaService).getBinaryFile(eq("localhost"), eq(null), eq("some.unknown_filetype"));

        when(mediaService.getBinaryFile(eq("localhost"), eq("Bob"), eq("explosive.file"))).thenThrow(
                new IOException(""));
        this.mockMvc.perform(get("/_media/explosive.file").
                        principal(auth)).
                andExpect(status().isNotFound());
    }

    @Test
    void saveFile() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        MockMultipartFile mfile = new MockMultipartFile("file", "someFile.jpg", MediaType.TEXT_PLAIN_VALUE, "Hello, world".getBytes());

        this.mockMvc.perform(multipart("/_media/upload").file(mfile).principal(auth)).andExpect(status().isOk()).andExpect(content().string("Uploaded for Bob"));

        verify(mediaService).saveFile(eq("localhost"), eq("Bob"), eq(mfile));

        Authentication authJoe = new UsernamePasswordAuthenticationToken("Joe", "password1");
        Mockito.doThrow(new IOException("")).when(mediaService).saveFile(eq("localhost"), eq("Joe"), eq(mfile));
        this.mockMvc.perform(multipart("/_media/upload").file(mfile).principal(authJoe)).andExpect(status().isOk()).andExpect(content().string("oops"));
    }

    @Test
    void listFiles() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        MediaRecord file1 = new MediaRecord("file1.png", "default", "bob", 0, 0, 0);
        MediaRecord file2 = new MediaRecord("file2.jpg", "default", "bob", 0, 0, 0);
        when(mediaService.getAllFiles(any(), any())).thenReturn(List.of(file1, file2));

        this.mockMvc.perform(get("/_media/list").principal(auth)).andExpect(status().isOk()).andExpect(content().json(
                " [{\"fileName\":\"file1.png\"}, {\"fileName\":\"file2.jpg\"}]"));

        this.mockMvc.perform(get("/_media/list")).andExpect(status().isOk()).andExpect(content().json(
                " [{\"fileName\":\"file1.png\"}, {\"fileName\":\"file2.jpg\"}]"));

    }
}