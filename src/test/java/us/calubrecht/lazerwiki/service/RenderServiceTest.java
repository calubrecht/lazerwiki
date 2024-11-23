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
        PageData pd = new PageData(null, "This is raw page text",  null,null, null, PageData.ALL_RIGHTS, 1L);
        when(renderer.renderWithInfo(eq("This is raw page text"), eq("host1"), eq("default"), eq("ns:realPage"), anyString())).thenReturn(new RenderResult("This is Rendered Text", "", new HashMap<>()));
        when(pageService.getPageData(any(), eq("ns:realPage"), any())).thenReturn(pd);
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(pageService.adjustSource(anyString(), any())).thenReturn("adjusted Source");

        assertEquals(new PageData("This is Rendered Text", "adjusted Source",   null,null,null, PageData.ALL_RIGHTS, 1L), underTest.getRenderedPage("host1", "ns:realPage", "Bob"));

        PageData noPageData = new PageData("Doesn't exist", "This is raw page text",  null, null,new PageFlags(false, false, true, true, false));
        when(pageService.getPageData(any(), eq("ns:nonPage"), any())).thenReturn(noPageData);
        assertEquals(new PageData("Doesn't exist", "This is raw page text",   null,null, new PageFlags(false, false, true, true, false)), underTest.getRenderedPage("host1", "ns:nonPage", "Bob"));
    }

    @Test
    public void testRenderError() {
        PageData pd = new PageData(null, "This is raw page text",  null, null, PageData.ALL_RIGHTS);
        when(renderer.renderToString(eq("This is raw page text"), eq("host1"), eq("default"), anyString(), anyString())).thenThrow(new NullPointerException());
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
        when(renderer.renderWithInfo(eq("text"), eq("host"), eq("default"), anyString(), anyString())).thenReturn(
                new RenderResult("rendered", "", Map.of(RenderResult.RENDER_STATE_KEYS.TITLE.name(),"The Title")));
        underTest.savePage("host", "pageName", "text", Collections.emptyList(), 10,false, "user");

        verify(pageUpdateService).savePage("host", "pageName", 10L, "text",  Collections.emptyList(),  Collections.emptySet(), Collections.emptySet(),"The Title","user", false);
    }



    @Test
    public void testSavePageWithLinks() throws PageWriteException {
        List<String> links = List.of("page1", "page2");
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(renderer.renderWithInfo(eq("text"), eq("host"), eq("default"), anyString(), anyString())).thenReturn(
                new RenderResult("rendered", "", Map.of(RenderResult.RENDER_STATE_KEYS.TITLE.name(),"The Title", RenderResult.RENDER_STATE_KEYS.LINKS.name(), links)));
        underTest.savePage("host", "pageName", "text", Collections.emptyList(), 10,false, "user");
        verify(pageUpdateService).savePage("host", "pageName", 10L, "text",  Collections.emptyList(),  links,Collections.emptySet(),"The Title","user", false);

    }

    @Test
    public void testPreviewPage() {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(renderer.renderToString("goodSource", "localhost", "default", "thisPage<preview>", "Bob")).thenReturn("This rendered");
        assertEquals("This rendered", underTest.previewPage("localhost", "thisPage", "goodSource", "Bob").rendered());

        when(renderer.renderToString("brokenSource", "localhost", "default", "thisPage<preview>", "Bob")).thenThrow(new RuntimeException("This is broken"));
        assertEquals("<h1>Error</h1>\n" +
                "<div>There was an error rendering this page! Please contact an admin, or correct the markup</div>\n" +
                "<code>brokenSource</code>", underTest.previewPage("localhost", "thisPage", "brokenSource", "Bob").rendered());


    }

    @Test
    public void testGetRendererdPAge_Cached() {
        PageData pd = new PageData(null, "This is raw page text",  null,null, PageData.ALL_RIGHTS);
        when(renderer.renderWithInfo(eq("This is raw page text"), eq("host1"), eq("default"), anyString(), anyString())).thenReturn(new RenderResult("This is Rendered Text", "", new HashMap<>()));
        when(pageService.getPageData(any(), eq("ns:realPage"), any())).thenReturn(pd);
        when(pageService.getPageData(any(), eq("ns:realPage2"), any())).thenReturn(pd);
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        PageCache cached = new PageCache();
        cached.useCache = true;
        cached.renderedCache = "This is from rendered Cache";
        cached.source = "Cached source";
        when(pageService.getCachedPage("host1", "ns:realPage")).thenReturn(cached);

        assertEquals(new PageData("This is from rendered Cache", "Cached source",   null,null,PageData.ALL_RIGHTS), underTest.getRenderedPage("host1", "ns:realPage", "Bob"));
        PageCache ignoreCache = new PageCache();
        ignoreCache.useCache = false;
        ignoreCache.renderedCache = "This is from rendered Cache";
        when(pageService.getCachedPage("host1", "ns:realPage2")).thenReturn(ignoreCache);
        when(pageService.adjustSource(anyString(), any())).thenReturn("adjusted Text");

        assertEquals(new PageData("This is Rendered Text", "adjusted Text",   null,null,PageData.ALL_RIGHTS), underTest.getRenderedPage("host1", "ns:realPage2", "Bob"));
    }

    @Test
    public void testGetHistoricalRenderedPage() {
        PageData pd = new PageData(null, "This is raw page text",  null,null, PageData.ALL_RIGHTS);
        when(renderer.renderWithInfo(eq("This is raw page text"), eq("host1"), eq("default"), anyString(), anyString())).thenReturn(new RenderResult("This is Rendered Text", "", new HashMap<>()));
        when(pageService.getHistoricalPageData(any(), eq("ns:realPage"), eq(1L), any())).thenReturn(pd);
        when(siteService.getSiteForHostname(any())).thenReturn("default");

        assertEquals(new PageData("This is Rendered Text", "This is raw page text",   null,null,PageData.ALL_RIGHTS), underTest.getHistoricalRenderedPage("host1", "ns:realPage", 1L,"Bob"));

        PageData noPageData = new PageData("Doesn't exist", "This is raw page text",  null, null,new PageFlags(false, false, true, true, false));
        when(pageService.getHistoricalPageData(any(), eq("ns:nonPage"), anyLong(), any())).thenReturn(noPageData);
        assertEquals(new PageData("Doesn't exist", "This is raw page text",   null,null, new PageFlags(false, false, true, true, false)), underTest.getHistoricalRenderedPage("host1", "ns:nonPage", 1L,"Bob"));

        PageData nonPermissioned = new PageData("Not for you", "This is raw page text",  null,null, new PageFlags(true, false, false, false, false));
        when(pageService.getHistoricalPageData(any(), eq("no"), anyLong(), any())).thenReturn(nonPermissioned);
        assertEquals(new PageData("Not for you", "This is raw page text",   null,null, new PageFlags(true, false, false, false, false)), underTest.getHistoricalRenderedPage("host1", "no", 1L,"Bob"));

        PageData badRender = new PageData(null, "BAD",  null,null, PageData.ALL_RIGHTS);
        when(pageService.getHistoricalPageData(any(), eq("badRender"), anyLong(), any())).thenReturn(badRender);
        when(renderer.renderWithInfo(eq("BAD"), any(), any(), any(), any())).thenThrow(new RuntimeException("OUTCH"));
        assertEquals(new PageData("""
                <h1>Error</h1>
                <div>There was an error rendering this page! Please contact an admin, or correct the markup</div>
                <code>BAD</code>""", "BAD",   null,null,  PageData.ALL_RIGHTS), underTest.getHistoricalRenderedPage("host1", "badRender", 1L,"Bob"));
    }
}
