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

        assertEquals("This Page Exists", doRender("[[exists| ]]"));
    }

    @Test
    void testRenderList() {
        assertEquals("Item 1\nItem2", doRender("  *Item 1\n  *Item2"));

    }

    @Test
    void testRenderHeader() {
        assertEquals("The Header", doRender("===The Header==="));

        assertEquals("The Header with a link", doRender("===The Header with a [[goHere|link]]==="));

    }

    @Test
    void testRenderImg() {
        assertEquals("thisImage.jpg", doRender("{{thisImage.jpg}}"));

        assertEquals("Image with a title", doRender("{{thisImage.jpg|Image with a title}}"));

    }

    @Test
    void testRenderCodeBox() {
        assertEquals("This is in the code block", doRender("  This is in the code block"));
    }

    @Test
    void testRenderMacro() {
        assertEquals("", doRender("~~MACRO~~This could be any macro~~/MACRO~~"));
    }

    @Test
    void testRenderSpans() {
        assertEquals("Bold, italic, none of it is rendered", doRender("**Bold, //italic//**, none of it is rendered"));
    }

    @Test
    public void testRenderLinebreak() {
        String input = "This is a line \\\\ with a linebreak";
        assertEquals("This is a line\n with a linebreak", doRender(input));
    }

    @Test
    public void testRenderTable() {
        String inputSimpleTable = "| First | Line |\n|Second | Line|";
        assertEquals("| First | Line |\n" +
                "|Second | Line|", doRender(inputSimpleTable));
    }

    @Test
    public void testRenderBlockquote() {
        String inputBlockquote = "> One quote **with some bold**\n>And\n>>Another layer of quote";
        assertEquals(" One quote with some bold\nAnd\nAnother layer of quote", doRender(inputBlockquote));

    }
}
