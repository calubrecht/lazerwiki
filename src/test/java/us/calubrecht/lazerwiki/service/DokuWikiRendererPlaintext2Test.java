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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = { CustomWikiRenderer.class, DokuWikiRendererPlaintext2Test.TestConfig.class})
@ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
@ActiveProfiles("test")
public class DokuWikiRendererPlaintext2Test {
    @MockBean
    TOCRenderService tocRenderService;

    @Configuration
    @ComponentScan({"us.calubrecht.lazerwiki.syntax"})
    public static class TestConfig {
    }

    @Autowired
    CustomWikiRenderer underTest;

    @MockBean
    PageService pageService;

    @MockBean
    MacroService macroService;

    @MockBean
    RandomService randomService;

    @MockBean
    LinkOverrideService linkOverrideService;

    @MockBean
    MediaOverrideService mediaOverrideService;

    String doRender(String source) {
        RenderContext context = new RenderContext("localhost", "default", "page", "jack");
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
        when(macroService.renderMacro(anyString(), anyString(), any())).thenReturn("Something that won't show in plaintext");
        assertEquals("", doRender("~~MACRO~~This could be any macro~~/MACRO~~"));
    }

    @Test
    void testRenderSpans() {
        assertEquals("Bold, italic, none of it is rendered", doRender("**Bold, //italic//**, none of it is rendered"));
    }

    @Test
    public void testRenderUnformat() {
        String input1 = "%%This **should not be bold**%%";
        assertEquals("This **should not be bold**", doRender(input1));
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

    @Test
    public void testHidden() {
        when(randomService.nextInt()).thenReturn(5,8);
        String inputBlockquote = "<hidden>simple</hidden>";
        assertEquals("simple", doRender(inputBlockquote));

        assertEquals("line1\n\nline2with some title text",
                doRender("<hidden>line1\n\nline2{{animage|with some title text}}</hidden>"));

        String namedHidden = "<hidden name=\"Bark\">Something in  here</hidden>";
        assertEquals("Bark:Something in  here", doRender(namedHidden));
    }

    @Test
    public void testRenderNoTOC() {
        String source = "====== Header 1 ======\n ==== Header 2 ====\n====== Header 3 ======\n===== Header 2 =====\n  ~~NOTOC~~";
        String headerRender = """
                <div id="lw_TOC"></div>
                """;

        when(tocRenderService.renderTOC(any(), any())).thenReturn(headerRender);

        assertEquals(" Header 1 \n Header 2 \n Header 3 \n Header 2 ", doRender(source));
    }

    @Test
    public void testRenderBrokenInput() {
        String source="---";
        // If can't figure it out, just print it raw
        assertEquals("---", doRender(source));
    }

    @Test
    public void testRenderHR() {
        String source="----";

        assertEquals("----", doRender(source));
        source = "-----";
        assertEquals("-----", doRender(source));
    }

    @Test
    public void testHtmlEntities() {
        String source="<Ä   #";

        // Plaintext renderer should not replace with entities
        assertEquals(source, doRender(source));
    }
}
