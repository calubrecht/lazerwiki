package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    IMarkupRenderer renderer;

    @MockBean
    SiteService siteService;

    @Test
    public void testRender() {
        PageData pd = new PageData(null, "This is raw page text",  null,true, true, true);
        when(renderer.renderToString(eq("This is raw page text"), eq("host1"), eq("default"))).thenReturn("This is Rendered Text");
        when(pageService.getPageData(any(), eq("ns:realPage"), any())).thenReturn(pd);
        when(siteService.getSiteForHostname(any())).thenReturn("default");

        assertEquals(new PageData("This is Rendered Text", "This is raw page text",   null,true, true, true), underTest.getRenderedPage("host1", "ns:realPage", "Bob"));

        PageData noPageData = new PageData("Doesn't exist", "This is raw page text",  null, false, true, true);
        when(pageService.getPageData(any(), eq("ns:nonPage"), any())).thenReturn(noPageData);
        assertEquals(new PageData("Doesn't exist", "This is raw page text",   null,false, true, true), underTest.getRenderedPage("host1", "ns:nonPage", "Bob"));
    }

    @Test
    public void testRenderError() {
        PageData pd = new PageData(null, "This is raw page text",  null, true, true, true);
        when(renderer.renderToString(eq("This is raw page text"), eq("host1"), eq("default"))).thenThrow(new NullPointerException());
        when(pageService.getPageData(any(), eq("ns:realPage"), any())).thenReturn(pd);
        when(siteService.getSiteForHostname(any())).thenReturn("default");

        assertEquals(new PageData("<h1>Error</h1>\n<div>There was an error rendering this page! Please contact an admin, or correct the markup</div>\n<code>This is raw page text</code>", "This is raw page text",   null,true, true, true), underTest.getRenderedPage("host1", "ns:realPage", "Bob"));

    }

    @Test
    public void testRenderCantREad() {
        PageData pd = new PageData("Can't read this", "Can't read this",   null,true, false, false);
        when(pageService.getPageData(any(), eq("ns:realPage"), any())).thenReturn(pd);

        assertEquals(new PageData("Can't read this", "Can't read this",   null,true, false, false), underTest.getRenderedPage("host1", "ns:realPage", "Bob"));

    }

    @Test
    public void testSavePage() throws PageWriteException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(renderer.renderWithInfo("text", "host", "default")).thenReturn(
                new RenderResult("rendered", "The Title", null));
        underTest.savePage("host", "pageName", "text", Collections.emptyList(), "user");

        verify(pageService).savePage("host", "pageName", "text",  Collections.emptyList(), "The Title","user");
    }
}
