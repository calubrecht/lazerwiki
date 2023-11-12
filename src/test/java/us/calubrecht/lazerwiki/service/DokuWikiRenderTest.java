package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = { DokuWikiRenderer.class, RendererRegistrar.class, DokuWikiRendererTest.TestConfig.class})
@ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
@ActiveProfiles("test")
class DokuWikiRendererTest {

    @Configuration
    @ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
    public static class TestConfig {
    }

    @Autowired
    DokuWikiRenderer underTest;

    @MockBean
    PageService pageService;

    @Test
    void testRenderHeader() {
        String source = "====== Big header ======\n==== Smaller Header ====";

        assertEquals("<h1>Big header</h1>\n<h3>Smaller Header</h3>", underTest.render(source));

        assertEquals("<h2>Header with space.</h2>", underTest.render("=====Header with space.===== "));

        assertEquals("===Doesn't parse as header=== with trailing", underTest.render("===Doesn't parse as header=== with trailing"));
    }


    @Test
    void testRenderLink() {
        when(pageService.exists(eq("default"), eq("exists"))).thenReturn(true);
        assertEquals("<a class=\"wikiLinkMissing\" href=\"/page/missing\">This link is missing</a>", underTest.render("[[missing|This link is missing]]"));
        assertEquals("<a class=\"wikiLink\" href=\"/page/exists\">This link exists</a>", underTest.render("[[exists|This link exists]]"));

        when(pageService.getTitle(eq("default"), eq("exists"))).thenReturn("This Page Exists");
        when(pageService.getTitle(eq("default"), eq("someNamespace:missing"))).thenReturn("missing");
        // Without link description
        assertEquals("<a class=\"wikiLinkMissing\" href=\"/page/someNamespace:missing\">missing</a>", underTest.render("[[someNamespace:missing ]]"));
        assertEquals("<a class=\"wikiLink\" href=\"/page/exists\">This Page Exists</a>", underTest.render("[[exists]]"));
        assertEquals("<a class=\"wikiLinkExternal\" href=\"http://somewhere.com\">http://somewhere.com</a>", underTest.render("[[http://somewhere.com]]"));

        // Nested in header
        assertEquals("<h1>Some text in <a class=\"wikiLinkMissing\" href=\"/page/headerLink\">a header</a></h1>", underTest.render("======Some text in [[ headerLink |a header]]======"));
    }

    @Test
    public void testRenderLinkSanitizesLinks() {
        String linkEmbeddingJS = "[[what \" onclick=\"doEvil|This link may be evil]]";
        assertEquals("<a class=\"wikiLinkMissing\" href=\"/page/what_onclick_doEvil\">This link may be evil</a>", underTest.render(linkEmbeddingJS));

        // External Link
        assertEquals("<a class=\"wikiLinkExternal\" href=\"http://malformed.invalid\">http://malformed.invalid</a>", underTest.render("[[https://ListGoesWhere\" onclick=\"evil"));

    }

    @Test
    public void testRenderLinkToHome() {
        when(pageService.getTitle(eq("default"), eq(""))).thenReturn("Home");
        when(pageService.exists(eq("default"), eq(""))).thenReturn(true);
        assertEquals("<a class=\"wikiLink\" href=\"/\">Home</a>", underTest.render("[[]]"));
        assertEquals("<a class=\"wikiLink\" href=\"/\">Name of Home</a>", underTest.render("[[|Name of Home]]"));
    }

    @Test
    public void testRenderSanitizeHtmlInText() {
        assertEquals("This &lt;b&gt;source&lt;/b&gt; has markup and &lt;script&gt;console.log(\"hey buddy\");&lt;/script&gt;", underTest.render("This <b>source</b> has markup and <script>console.log(\"hey buddy\");</script>"));

        assertEquals("Escape &lt;b&gt;this&lt;/b&gt; but not <a class=\"wikiLinkMissing\" href=\"/page/aLink\">a link</a> and &lt;b&gt;escape&lt;/b&gt; again", underTest.render("Escape <b>this</b> but not [[ aLink | a link]] and <b>escape</b> again"));

    }

    @Test
    public void testCanGetDefaultRendererForUnknownClass() {
        assertEquals(null, underTest.renderers.getRenderer(Integer.class).getTarget());
    }
}