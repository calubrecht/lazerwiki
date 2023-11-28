package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = { DokuWikiRenderer.class, RendererRegistrar.class, DokuWikiRendererTest.TestConfig.class})
@ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
@ActiveProfiles("test")
public class DokuWikiRendererPlaintextTest {

    @Configuration
    @ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
    public static class TestConfig {
    }

    @Autowired
    DokuWikiRenderer underTest;

    @MockBean
    PageService pageService;

    @MockBean
    MacroService macroService;

    String doRender(String source) {
        RenderContext context = new RenderContext("localhost", "default", "jack");
        return underTest.renderToPlainText(source, context);
    }


    @Test
    void testRenderLink() {

        when(pageService.exists(eq("localhost"), eq("exists"))).thenReturn(true);
        when(pageService.getTitle(eq("localhost"), eq("exists"))).thenReturn("This Page Exists");

        assertEquals("This Page Exists", doRender("[[exists]]"));
        assertEquals("This Page has an alternate display", doRender("[[ exists|This Page has an alternate display]]"));
        assertEquals("httP://externalLink.com", doRender("[[httP://externalLink.com ]]"));
    }

    @Test
    void testRenderList() {
        assertEquals("Item 1\nItem2", doRender("  *Item 1\n  *Item2"));

    }
}
