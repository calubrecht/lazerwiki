package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.service.PageService;
import us.calubrecht.lazerwiki.service.PageUpdateService;
import us.calubrecht.lazerwiki.service.RenderService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PageController.class, VersionController.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class PageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PageService pageService;

    @MockBean
    PageUpdateService pageUpdateService;

    @MockBean
    RenderService renderService;

    @Test
    public void testGetPage() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        this.mockMvc.perform(get("/api/page/get/testPage").
                        principal(auth)).
                andExpect(status().isOk());

        verify(renderService).getRenderedPage(eq("localhost"), eq("testPage"), eq("Bob"));
    }

    @Test
    public void testGetPageAnon() throws Exception {
        this.mockMvc.perform(get("/api/page/get/testPage")).
                andExpect(status().isOk());

        verify(renderService).getRenderedPage(eq("localhost"), eq("testPage"), eq("Guest"));
    }

    @Test
    public void testGetPageHistory() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        this.mockMvc.perform(get("/api/page/history/testPage").
                        principal(auth)).
                andExpect(status().isOk());

        verify(pageService).getPageHistory(eq("localhost"), eq("testPage"), eq("Bob"));
    }

    @Test
    public void testSavePage() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        String data = "{\"pageName\": \"thisPage\", \"text\": \"This is some text\"}";
        this.mockMvc.perform(post("/api/page/testPage/savePage").
                        content(data).
                        contentType(MediaType.APPLICATION_JSON).
                        principal(auth)).
                andExpect(status().isOk());

        verify(renderService).savePage(eq("localhost"), eq("testPage"), eq("This is some text"), isNull(), eq("Bob"));
    }

    @Test
    public void testListPage() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        String data = "{\"pageName\": \"thisPage\", \"text\": \"This is some text\"}";
        this.mockMvc.perform(get("/api/page/listPages").
                        principal(auth)).
                andExpect(status().isOk());

        verify(pageService).getAllPages(eq("localhost"), eq("Bob"));

        this.mockMvc.perform(get("/api/page/listPages")).
                andExpect(status().isOk());

        verify(pageService).getAllPages(eq("localhost"), eq("Guest"));
    }

    @Test
    public void testListTags() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        when(pageService.getAllTags(any(), any())).thenReturn(List.of("tag1","tag2"));
        String data = "[\"tag1\", \"tag2\"]";
        this.mockMvc.perform(get("/api/page/listTags").
                        principal(auth)).
                andExpect(status().isOk()).andExpect(content().json(data));

        verify(pageService).getAllTags(eq("localhost"), eq("Bob"));

        this.mockMvc.perform(get("/api/page/listTags")).
                andExpect(status().isOk()).andExpect(content().json(data));

        verify(pageService).getAllTags(eq("localhost"), eq("Guest"));
    }

    @Test
    public void testSearchPages() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        this.mockMvc.perform(get("/api/page/searchPages?search=tag:common").
                        principal(auth)).
                andExpect(status().isOk());

        verify(pageService).searchPages(eq("localhost"), eq("Bob"), eq("tag:common"));

        this.mockMvc.perform(get("/api/page/searchPages?search=tag:common")).
                andExpect(status().isOk());

        verify(pageService).searchPages(eq("localhost"), eq("Guest"), eq("tag:common"));

    }

    @Test
    public void testDeletePage() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        this.mockMvc.perform(delete("/api/page/testPage").
                        principal(auth)).
                andExpect(status().isOk());

        verify(pageUpdateService).deletePage(eq("localhost"), eq("testPage"), eq("Bob"));
    }

    @Test
    public void testPreviewPage() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        String data = "{\"pageName\": \"thisPage\", \"text\": \"This is some text\"}";
        this.mockMvc.perform(post("/api/page/previewPage/thisPage").
                        content(data).
                        contentType(MediaType.APPLICATION_JSON).
                        principal(auth)).
                andExpect(status().isOk());


        verify(renderService).previewPage(eq("localhost"), eq("thisPage"), eq("This is some text"), eq("Bob"));
        this.mockMvc.perform(post("/api/page/previewPage/").
                        content(data).
                        contentType(MediaType.APPLICATION_JSON).
                        principal(auth)).
                andExpect(status().isOk());


        verify(renderService).previewPage(eq("localhost"), eq(""), eq("This is some text"), eq("Bob"));
    }

    /*
    @Test
    public void testGetPageRevisions() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
        String data = "{\"pageName\": \"thisPage\", \"text\": \"This is some text\"}";
        this.mockMvc.perform(get("/api/page/pageRevisions/thisPage").principal(auth)).
                andExpect(status().isOk());


        verify(pageService).pageRevisions(eq("localhost"), eq("thisPage"), eq("Bob"));
    }*/
}
