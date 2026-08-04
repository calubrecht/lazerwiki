package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {MarkdownWikiRenderer.class, MarkdownWikiRendererTest.TestConfig.class})
@ActiveProfiles("test")
class MarkdownWikiRendererTest {
    @Autowired
    MarkdownWikiRenderer underTest;

    @Configuration
    @ComponentScan({"us.calubrecht.lazerwiki.syntax"})
    public static class TestConfig {}

    @MockitoBean
    PageService pageService;

    @MockitoBean MacroService macroService;

    @MockitoBean RandomService randomService;

    @MockitoBean LinkOverrideService linkOverrideService;

    @MockitoBean MediaOverrideService mediaOverrideService;

    @MockitoBean TOCRenderService tocRenderService;

    String doRender(String source) {
        return underTest.renderToString(source, "default", "page", "");
    }
    @Test
    void test_renderHeader() {
        String source = "# Big header\n ### Smaller Header";

        assertEquals(
                "<h1 id=\"header_Big_header\">Big header</h1>\n<h3 id=\"header_Smaller_Header\">Smaller Header</h3>",
                doRender(source));

        assertEquals(
                "<h2 id=\"header_Header_with_space.\">Header with space.</h2>",
                doRender("## Header with space."));
    }

    @Test
    void test_renderPlaintext() {
        String source = "# Big header\n ### Smaller Header";

        RenderContext context = new RenderContext("default", "page", "jack");
        String text =  underTest.renderWithInfo(source, context).plainText();
        assertEquals("Big header\n" +
                "Smaller Header", text);
    }


    @Test
    void test_renderLink() {
        when(pageService.exists(eq("default"), eq("exists"))).thenReturn(true);
        assertEquals(
                "<div><a class=\"wikiLinkMissing\" href=\"/page/missing\">This link is missing</a></div>",
                doRender("[This link is missing](missing)"));
        assertEquals(
                "<div><a class=\"wikiLink\" href=\"/page/exists\">This link exists</a></div>",
                doRender("[This link exists](exists)"));

        when(pageService.getTitle(eq("default"), eq("exists"))).thenReturn("This Page Exists");
        when(pageService.getTitle(eq("default"), eq("someNamespace:missing"))).thenReturn("missing");
        // Without link description
        assertEquals(
                "<div><a class=\"wikiLinkMissing\" href=\"/page/someNamespace:missing\">missing</a></div>",
                doRender("[](someNamespace:missing)"));
        assertEquals(
                "<div><a class=\"wikiLink\" href=\"/page/exists\">This Page Exists</a></div>",
                doRender("[](exists)"));
        assertEquals(
                "<div><a class=\"wikiLinkExternal\" href=\"http://somewhere.com\">http://somewhere.com</a></div>",
                doRender("[](http://somewhere.com)"));

        // Nested in header
        assertEquals(
                "<h1 id=\"header_Some_text_in_a_header\">Some text in <a class=\"wikiLinkMissing\" href=\"/page/headerLink\">a header</a></h1>",
                doRender("# Some text in [a header](headerLink )"));

        // Link with dash in URL
        assertEquals(
                "<div><a class=\"wikiLinkExternal\" href=\"http://domain.example/a-page\">http://domain.example/a-page</a></div>",
                doRender("[](http://domain.example/a-page)"));

        // Link with blank link text (use title instead)
        assertEquals(
                "<div><a class=\"wikiLink\" href=\"/page/exists\">This Page Exists</a></div>",
                doRender("[](exists)"));
    }

    @Test
    public void test_macro() {
        String inputMacro = "~~MACRO~~macro1: macro~~/MACRO~~";
        when(macroService.renderMacro(eq("macro1: macro"), anyString(), any()))
                .thenReturn("<div>MACRO- Unknown Macro macro1</div>");
        String render = underTest.renderToString(inputMacro, "", "page", "");
        assertEquals("<div>MACRO- Unknown Macro macro1</div>", render);
    }

}