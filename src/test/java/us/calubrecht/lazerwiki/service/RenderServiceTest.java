package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.PageCache;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.responses.PageData.PageFlags;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {RenderService.class})
@ActiveProfiles("test")
public class RenderServiceTest {

    @Autowired
    RenderService underTest;

    @MockBean
    PageService pageService;

    @MockBean
    PageUpdateService pageUpdateService;

    @MockBean
    IMarkupRenderer renderer;

    @MockBean
    SiteService siteService;

    @Test
    public void testRender() {
        PageData pd = new PageData(null, "This is raw page text",  null,null, PageData.ALL_RIGHTS);
        when(renderer.renderWithInfo(eq("This is raw page text"), eq("host1"), eq("default"), anyString())).thenReturn(new RenderResult("This is Rendered Text", "", new HashMap<>()));
        when(pageService.getPageData(any(), eq("ns:realPage"), any())).thenReturn(pd);
        when(siteService.getSiteForHostname(any())).thenReturn("default");

        assertEquals(new PageData("This is Rendered Text", "This is raw page text",   null,null,PageData.ALL_RIGHTS), underTest.getRenderedPage("host1", "ns:realPage", "Bob"));

        PageData noPageData = new PageData("Doesn't exist", "This is raw page text",  null, null,new PageFlags(false, false, true, true, false));
        when(pageService.getPageData(any(), eq("ns:nonPage"), any())).thenReturn(noPageData);
        assertEquals(new PageData("Doesn't exist", "This is raw page text",   null,null, new PageFlags(false, false, true, true, false)), underTest.getRenderedPage("host1", "ns:nonPage", "Bob"));
    }

    @Test
    public void testRenderError() {
        PageData pd = new PageData(null, "This is raw page text",  null, null, PageData.ALL_RIGHTS);
        when(renderer.renderToString(eq("This is raw page text"), eq("host1"), eq("default"), anyString())).thenThrow(new NullPointerException());
        when(pageService.getPageData(any(), eq("ns:realPage"), any())).thenReturn(pd);
        when(siteService.getSiteForHostname(any())).thenReturn("default");

        assertEquals(new PageData("<h1>Error</h1>\n<div>There was an error rendering this page! Please contact an admin, or correct the markup</div>\n<code>This is raw page text</code>", "This is raw page text",   null,null,PageData.ALL_RIGHTS), underTest.getRenderedPage("host1", "ns:realPage", "Bob"));

    }

    @Test
    public void testRenderCantREad() {
        PageData pd = new PageData("Can't read this", "Can't read this",   null,null,new PageFlags(true, false, false, false, false));
        when(pageService.getPageData(any(), eq("ns:realPage"), any())).thenReturn(pd);

        assertEquals(new PageData("Can't read this", "Can't read this",   null,null,new PageFlags(true, false, false, false, false)), underTest.getRenderedPage("host1", "ns:realPage", "Bob"));

    }

    @Test
    public void testSavePage() throws PageWriteException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(renderer.renderWithInfo(eq("text"), eq("host"), eq("default"), anyString())).thenReturn(
                new RenderResult("rendered", "", Map.of(RenderResult.RENDER_STATE_KEYS.TITLE.name(),"The Title")));
        underTest.savePage("host", "pageName", "text", Collections.emptyList(), "user");

        verify(pageUpdateService).savePage("host", "pageName", "text",  Collections.emptyList(),  Collections.emptySet(),"The Title","user");
    }



    @Test
    public void testSavePageWithLinks() throws PageWriteException {
        List<String> links = List.of("page1", "page2");
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(renderer.renderWithInfo(eq("text"), eq("host"), eq("default"), anyString())).thenReturn(
                new RenderResult("rendered", "", Map.of(RenderResult.RENDER_STATE_KEYS.TITLE.name(),"The Title", RenderResult.RENDER_STATE_KEYS.LINKS.name(), links)));
        underTest.savePage("host", "pageName", "text", Collections.emptyList(), "user");
        verify(pageUpdateService).savePage("host", "pageName", "text",  Collections.emptyList(),  links,"The Title","user");

    }

    @Test
    public void testPreviewPage() {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(renderer.renderToString("goodSource", "localhost", "default", "Bob")).thenReturn("This rendered");
        assertEquals("This rendered", underTest.previewPage("localhost", "thisPage", "goodSource", "Bob").rendered());

        when(renderer.renderToString("brokenSource", "localhost", "default", "Bob")).thenThrow(new RuntimeException("This is broken"));
        assertEquals("<h1>Error</h1>\n" +
                "<div>There was an error rendering this page! Please contact an admin, or correct the markup</div>\n" +
                "<code>brokenSource</code>", underTest.previewPage("localhost", "thisPage", "brokenSource", "Bob").rendered());


    }

    @Test
    public void testGetRendererdPAge_Cached() {
        PageData pd = new PageData(null, "This is raw page text",  null,null, PageData.ALL_RIGHTS);
        when(renderer.renderWithInfo(eq("This is raw page text"), eq("host1"), eq("default"), anyString())).thenReturn(new RenderResult("This is Rendered Text", "", new HashMap<>()));
        when(pageService.getPageData(any(), eq("ns:realPage"), any())).thenReturn(pd);
        when(pageService.getPageData(any(), eq("ns:realPage2"), any())).thenReturn(pd);
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        PageCache cached = new PageCache();
        cached.useCache = true;
        cached.renderedCache = "This is from rendered Cache";
        when(pageService.getCachedPage("host1", "ns:realPage")).thenReturn(cached);

        assertEquals(new PageData("This is from rendered Cache", "This is raw page text",   null,null,PageData.ALL_RIGHTS), underTest.getRenderedPage("host1", "ns:realPage", "Bob"));
        PageCache ignoreCache = new PageCache();
        ignoreCache.useCache = false;
        ignoreCache.renderedCache = "This is from rendered Cache";
        when(pageService.getCachedPage("host1", "ns:realPage2")).thenReturn(ignoreCache);

        assertEquals(new PageData("This is Rendered Text", "This is raw page text",   null,null,PageData.ALL_RIGHTS), underTest.getRenderedPage("host1", "ns:realPage2", "Bob"));


    }
}
