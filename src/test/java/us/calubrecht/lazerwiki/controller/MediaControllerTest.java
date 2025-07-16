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
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.responses.MediaListResponse;
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.responses.NsNode;
import us.calubrecht.lazerwiki.service.MediaService;
import us.calubrecht.lazerwiki.service.exception.MediaReadException;
import us.calubrecht.lazerwiki.service.exception.MediaWriteException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
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

        verify(mediaService).getBinaryFile(eq("localhost"), eq("Bob"), eq("someFile.jpg"), isNull());

        this.mockMvc.perform(get("/_media/some.unknown_filetype")).
                andExpect(status().isOk());
        verify(mediaService).getBinaryFile(eq("localhost"), eq(null), eq("some.unknown_filetype"), isNull());

        when(mediaService.getBinaryFile(eq("localhost"), eq("Bob"), eq("explosive.file"), isNull())).thenThrow(
                new IOException(""));
        this.mockMvc.perform(get("/_media/explosive.file").
                        principal(auth)).
                andExpect(status().isNotFound());

        Authentication authJoe = new UsernamePasswordAuthenticationToken("Joe", "password1");
        when(mediaService.getBinaryFile(eq("localhost"), eq("Joe"), any(), isNull())).thenThrow(
                new MediaReadException(""));
        this.mockMvc.perform(get("/_media/forbidden.file").
                        principal(authJoe)).
                andExpect(status().isForbidden());
    }

    @Test
    public void getFile_WithSize() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        this.mockMvc.perform(get("/_media/someFile.jpg?10x10").
                        principal(auth)).
                andExpect(status().isOk());

        verify(mediaService).getBinaryFile(eq("localhost"), eq("Bob"), eq("someFile.jpg"), eq("10x10"));
    }

    @Test
    void saveFile() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        MockMultipartFile mfile = new MockMultipartFile("file", "someFile.jpg", MediaType.TEXT_PLAIN_VALUE, "Hello, world".getBytes());

        this.mockMvc.perform(multipart("/_media/upload").file(mfile).param("namespace", "ns").principal(auth)).andExpect(status().isOk()).andExpect(content().string("Upload successful"));

        verify(mediaService).saveFile(eq("localhost"), eq("Bob"), eq(mfile), eq("ns"));

        Authentication authJoe = new UsernamePasswordAuthenticationToken("Joe", "password1");
        Mockito.doThrow(new MediaWriteException("")).when(mediaService).saveFile(eq("localhost"), eq("Joe"), eq(mfile), eq("ns"));
        this.mockMvc.perform(multipart("/_media/upload").file(mfile).param("namespace", "ns").principal(authJoe)).andExpect(status().isForbidden());
    }

    @Test
    void listFiles() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        User user = new User("Bob", "hash");
        MediaRecord file1 = new MediaRecord("file1.png", "default", "",user, 0, 0, 0);
        MediaRecord file2 = new MediaRecord("file2.jpg", "default", "", user, 0, 0, 0);
        MediaListResponse resp = new MediaListResponse(Map.of("", List.of(file1, file2)), new NsNode("", true));
        when(mediaService.getAllFiles(any(), any())).thenReturn(resp);

        this.mockMvc.perform(get("/_media/list").principal(auth)).andExpect(status().isOk()).andExpect(content().json(
                " { \"media\":{\"\":[{\"fileName\":\"file1.png\"}, {\"fileName\":\"file2.jpg\"}]}, \"namespaces\":{\"namespace\":\"\",\"children\":[], \"writable\":true}}"));


        this.mockMvc.perform(get("/_media/list")).andExpect(status().isOk()).andExpect(content().json(
                " { \"media\":{\"\":[{\"fileName\":\"file1.png\"}, {\"fileName\":\"file2.jpg\"}]}, \"namespaces\":{\"namespace\":\"\",\"children\":[], \"writable\":true}}"));

    }

    @Test
    void testDeleteFile() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        this.mockMvc.perform(delete("/_media/delete.file.jpg").principal(auth)).andExpect(status().isOk());
        verify(mediaService).deleteFile("localhost", "delete.file.jpg", "Bob");

        Mockito.doThrow(new MediaWriteException("")).when(mediaService).deleteFile(eq("localhost"), eq("unauthfile.jpg"), eq("Joe"));
        Authentication authJoe = new UsernamePasswordAuthenticationToken("Joe", "password1");
        this.mockMvc.perform(delete("/_media/unauthfile.jpg").principal(authJoe)).andExpect(status().isForbidden());
    }

    @Test
    void testRecentChanges() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        this.mockMvc.perform(get("/_media/recentChanges").
                        principal(auth)).
                andExpect(status().isOk());

        verify(mediaService).getRecentChanges(eq("localhost"), eq("Bob"));

        this.mockMvc.perform(get("/_media/recentChanges")).
                andExpect(status().isOk());
        verify(mediaService).getRecentChanges(eq("localhost"), eq("Guest"));
    }

    @Test
    void testMoveFile() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        String data = "{\"oldNS\": \"ns1\", \"oldFile\": \"img.jpg\", \"newNS\": \"ns2\", \"newFile\": \"img2.jpg\"}";
        this.mockMvc.perform(post("/_media/moveFile").
                        content(data).
                        contentType(MediaType.APPLICATION_JSON).
                        principal(auth)).
                andExpect(status().isOk());

        verify(mediaService).moveImage("localhost", "Bob", "ns1", "img.jpg", "ns2", "img2.jpg");

        // Check error.
        when(mediaService.moveImage("localhost", "Bob", "ns1", "img.jpg", "ns2", "img3.jpg")).thenThrow(
                new MediaWriteException("File already exists"));
        String data2 = "{\"oldNS\": \"ns1\", \"oldFile\": \"img.jpg\", \"newNS\": \"ns2\", \"newFile\": \"img3.jpg\"}";
        this.mockMvc.perform(post("/_media/moveFile").
                        content(data2).
                        contentType(MediaType.APPLICATION_JSON).
                        principal(auth)).
                andExpect(status().isOk()).andExpect(content().json("{\"success\": false, \"message\": \"File already exists\"}"));
    }
}