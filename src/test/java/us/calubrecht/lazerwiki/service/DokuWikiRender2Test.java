package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.framework.ParserRegistrar;
import us.calubrecht.lazerwiki.syntax.parser.HeaderParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@SpringBootTest(classes = { CustomWikiRenderer.class, DokuWikiRender2Test.TestConfig.class})
@ActiveProfiles("test")
public class DokuWikiRender2Test {
    @Autowired
    CustomWikiRenderer underTest;

    @Configuration
    @ComponentScan({"us.calubrecht.lazerwiki.syntax"})
    public static class TestConfig {
    }

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
        return underTest.renderToString(source, "localhost", "default", "page", "");
    }

    @Test
    void testRenderHeader() {
        String source = "====== Big header ======\n ==== Smaller Header ====";

        assertEquals("<h1 id=\"header_Big_header\">Big header</h1>\n<h3 id=\"header_Smaller_Header\">Smaller Header</h3>", doRender(source));

        assertEquals("<h2 id=\"header_Header_with_space.\">Header with space.</h2>", doRender("=====Header with space.===== "));

        assertEquals("<div>===Doesn't parse as header=== with trailing</div>", doRender("===Doesn't parse as header=== with trailing"));
    }

    @Test
    void testRenderLink() {
        when(pageService.exists(eq("localhost"), eq("exists"))).thenReturn(true);
        assertEquals("<div><a class=\"wikiLinkMissing\" href=\"/page/missing\">This link is missing</a></div>", doRender("[[missing|This link is missing]]"));
        assertEquals("<div><a class=\"wikiLink\" href=\"/page/exists\">This link exists</a></div>", doRender("[[exists|This link exists]]"));

        when(pageService.getTitle(eq("localhost"), eq("exists"))).thenReturn("This Page Exists");
        when(pageService.getTitle(eq("localhost"), eq("someNamespace:missing"))).thenReturn("missing");
        // Without link description
        assertEquals("<div><a class=\"wikiLinkMissing\" href=\"/page/someNamespace:missing\">missing</a></div>", doRender("[[someNamespace:missing ]]"));
        assertEquals("<div><a class=\"wikiLink\" href=\"/page/exists\">This Page Exists</a></div>", doRender("[[exists]]"));
        assertEquals("<div><a class=\"wikiLinkExternal\" href=\"http://somewhere.com\">http://somewhere.com</a></div>", doRender("[[http://somewhere.com]]"));

        // Nested in header
        assertEquals("<h1 id=\"header_Some_text_in_a_header\">Some text in <a class=\"wikiLinkMissing\" href=\"/page/headerLink\">a header</a></h1>", doRender("======Some text in [[ headerLink |a header]]======"));

        // Link with dash in URL
        assertEquals("<h1 id=\"header_Some_text_in_a_header\">Some text in <a class=\"wikiLinkMissing\" href=\"/page/headerLink\">a header</a></h1>", doRender("======Some text in [[ headerLink |a header]]======"));
        assertEquals("<div><a class=\"wikiLinkExternal\" href=\"http://domain.example/a-page\">http://domain.example/a-page</a></div>", doRender("[[http://domain.example/a-page]]"));

        // Link with blank link text (use title instead)
        assertEquals("<div><a class=\"wikiLink\" href=\"/page/exists\">This Page Exists</a></div>", doRender("[[exists|]]"));

        // broken link syntax
        assertEquals("<div>[[not quite a link]</div>", doRender("[[not quite a link]"));
    }
}
