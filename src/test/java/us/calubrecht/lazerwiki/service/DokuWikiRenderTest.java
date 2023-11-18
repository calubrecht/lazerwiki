package us.calubrecht.lazerwiki.service;

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

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
        String source = "====== Big header ======\n ==== Smaller Header ====";

        assertEquals("<h1>Big header</h1>\n<h3>Smaller Header</h3>", underTest.render(source));

        assertEquals("<h2>Header with space.</h2>", underTest.render("=====Header with space.===== "));

        assertEquals("<div>===Doesn't parse as header=== with trailing</div>", underTest.render("===Doesn't parse as header=== with trailing"));
    }


    @Test
    void testRenderLink() {
        when(pageService.exists(eq("default"), eq("exists"))).thenReturn(true);
        assertEquals("<div><a class=\"wikiLinkMissing\" href=\"/page/missing\">This link is missing</a></div>", underTest.render("[[missing|This link is missing]]"));
        assertEquals("<div><a class=\"wikiLink\" href=\"/page/exists\">This link exists</a></div>", underTest.render("[[exists|This link exists]]"));

        when(pageService.getTitle(eq("default"), eq("exists"))).thenReturn("This Page Exists");
        when(pageService.getTitle(eq("default"), eq("someNamespace:missing"))).thenReturn("missing");
        // Without link description
        assertEquals("<div><a class=\"wikiLinkMissing\" href=\"/page/someNamespace:missing\">missing</a></div>", underTest.render("[[someNamespace:missing ]]"));
        assertEquals("<div><a class=\"wikiLink\" href=\"/page/exists\">This Page Exists</a></div>", underTest.render("[[exists]]"));
        assertEquals("<div><a class=\"wikiLinkExternal\" href=\"http://somewhere.com\">http://somewhere.com</a></div>", underTest.render("[[http://somewhere.com]]"));

        // Nested in header
        assertEquals("<h1>Some text in <a class=\"wikiLinkMissing\" href=\"/page/headerLink\">a header</a></h1>", underTest.render("======Some text in [[ headerLink |a header]]======"));
    }

    @Test
    public void testRenderLinkSanitizesLinks() {
        String linkEmbeddingJS = "[[what \" onclick=\"doEvil|This link may be evil]]";
        assertEquals("<div><a class=\"wikiLinkMissing\" href=\"/page/what_onclick_doEvil\">This link may be evil</a></div>", underTest.render(linkEmbeddingJS));

        // External Link
        assertEquals("<div><a class=\"wikiLinkExternal\" href=\"http://malformed.invalid\">http://malformed.invalid</a></div>", underTest.render("[[https://ListGoesWhere\" onclick=\"evil"));

    }

    @Test
    public void testRenderLinkToHome() {
        when(pageService.getTitle(eq("default"), eq(""))).thenReturn("Home");
        when(pageService.exists(eq("default"), eq(""))).thenReturn(true);
        assertEquals("<div><a class=\"wikiLink\" href=\"/\">Home</a></div>", underTest.render("[[]]"));
        assertEquals("<div><a class=\"wikiLink\" href=\"/\">Name of Home</a></div>", underTest.render("[[|Name of Home]]"));
    }

    @Test
    public void testRenderSanitizeHtmlInText() {
        assertEquals("<div>This &lt;b&gt;source&lt;/b&gt; has markup and &lt;script&gt;console.log(\"hey buddy\");&lt;/script&gt;</div>", underTest.render("This <b>source</b> has markup and <script>console.log(\"hey buddy\");</script>"));

        assertEquals("<div>Escape &lt;b&gt;this&lt;/b&gt; but not <a class=\"wikiLinkMissing\" href=\"/page/aLink\"> a link</a> and &lt;b&gt;escape&lt;/b&gt; again</div>", underTest.render("Escape <b>this</b> but not [[ aLink | a link]] and <b>escape</b> again"));

    }

    @Test
    public void testCanGetDefaultRendererForUnknownClass() {
        assertEquals(null, underTest.renderers.getRenderer(Integer.class).getTargets());
    }

    @Test
    public void testLinebreaks() {
        String input1 = "A single linebreak in the source\nwill not break in the output";
        assertEquals("<div>A single linebreak in the source\nwill not break in the output</div>", underTest.render(input1));

        String input2 = "A double linebreak in the source\n\nbreaks in to paragraphs";
        assertEquals("<div>A double linebreak in the source</div>\n<div>breaks in to paragraphs</div>", underTest.render(input2));
    }

    @Test
    public void testRenderBold() {
        String input1 = "Some words are **meant to **be bold.";
        assertEquals("<div>Some words are <span class=\"bold\">meant to </span>be bold.</div>", underTest.render(input1));

        String input2 = "Some bolds **have [[link|links]] **";
        assertEquals("<div>Some bolds <span class=\"bold\">have <a class=\"wikiLinkMissing\" href=\"/page/link\">links</a> </span></div>", underTest.render(input2));

        String input3 = "Some bolds **aren't matched";
        assertEquals("<div>Some bolds **aren't matched</div>", underTest.render(input3));
        String input4 = "Can **bold\nspan lines?**";
        assertEquals("<div>Can <span class=\"bold\">bold\nspan lines?</span></div>", underTest.render(input4));
    }

    @Test
    public void testRenderItalic() {
        String input1 = "Some words are //meant to //be italic.";
        assertEquals("<div>Some words are <span class=\"italic\">meant to </span>be italic.</div>", underTest.render(input1));

        String input2 = "Some italics //have [[link|links]] //";
        assertEquals("<div>Some italics <span class=\"italic\">have <a class=\"wikiLinkMissing\" href=\"/page/link\">links</a> </span></div>", underTest.render(input2));

        String input3 = "Some italics //aren't matched";
        assertEquals("<div>Some italics //aren't matched</div>", underTest.render(input3));
        String input4 = "Can //italic\nspan lines?//";
        assertEquals("<div>Can <span class=\"italic\">italic\nspan lines?</span></div>", underTest.render(input4));
        String input5 = "Can **//italic be in// bold**?";
        assertEquals("<div>Can <span class=\"bold\"><span class=\"italic\">italic be in</span> bold</span>?</div>", underTest.render(input5));
        String input6 = "Can //**bold be in** italic//?";
        assertEquals("<div>Can <span class=\"italic\"><span class=\"bold\">bold be in</span> italic</span>?</div>", underTest.render(input6));
    }

    @Test
    public void testRenderImage() {
        String input1 = "{{img.jpg}}";
        assertEquals(
                "<div><img src=\"/_media/img.jpg\" class=\"media\" loading=\"lazy\"></div>",
                underTest.render(input1)
        );

        // Image inside link
        String input2 = "[[somePage|w {{img.jpg}} y]]";
        assertEquals(
                "<div><a class=\"wikiLinkMissing\" href=\"/page/somePage\">w <img src=\"/_media/img.jpg\" class=\"media\" loading=\"lazy\"> y</a></div>",
                underTest.render(input2)
        );

        // Image with dash in name
        String input3 = "{{an-image.jpg}}";
        assertEquals(
                "<div><img src=\"/_media/an-image.jpg\" class=\"media\" loading=\"lazy\"></div>",
                underTest.render(input3)
        );
    }

    @Test
    public void testRenderUList() {
        String input1 = " * Simple List\n *With 2 rows\nThen * non-matching\n";
        assertEquals(
                "<div><ul>\n<li>Simple List</li>\n<li>With 2 rows</li>\n</ul>\nThen * non-matching</div>",
                underTest.render(input1)
        );

        // List after blank line
        String input2 = "Something\n\n * Simple List\n *With 2 rows\nThen * non-matching\n";
        assertEquals(
                "<div>Something</div>\n<div><ul>\n<li>Simple List</li>\n<li>With 2 rows</li>\n</ul>\nThen * non-matching</div>",
                underTest.render(input2)
        );
    }

    @Test
    public void testRenderOList() {
        String input1 = " - Simple List\n -With 2 rows\nThen * non-matching\n";
        assertEquals(
                "<div><ol>\n<li>Simple List</li>\n<li>With 2 rows</li>\n</ol>\nThen * non-matching</div>",
                underTest.render(input1)
        );
    }

    @Test
    public void testRenderNestedLists() {
        String input1 = " - Simple List\n  -Deeper List\n   * DeepestList\n";
        assertEquals(
                "<div><ol>\n<li>Simple List</li>\n<ol>\n<li>Deeper List</li>\n<ul>\n<li>DeepestList</li>\n</ul>\n</ol>" +
                        "\n</ol></div>",
                underTest.render(input1)
        );

        String input2 = " - Simple List\n *List Changes Type\n   * DeepestList\n * and backout\n";
        assertEquals(
                "<div><ol>\n<li>Simple List</li>\n</ol>\n<ul>\n<li>List Changes Type</li>\n<ul>\n<li>DeepestList</li>\n" +
                        "</ul>\n<li>and backout</li>\n</ul></div>",
                underTest.render(input2)
        );
    }

    @Test
    public void testCodeBlock() {
        String input1 = "  This is a block\n  Should all be one block\n   with more spaces?\n";
        assertEquals(
                "<pre class=\"code\">This is a block\nShould all be one block\n with more spaces?\n</pre>",
                underTest.render(input1)
        );

        String input2 = "**bold on one line**\n  Raw text box, do not render **bold things**\n";
        assertEquals(
                "<div><span class=\"bold\">bold on one line</span></div><pre class=\"code\">Raw text box, do not render **bold things**\n</pre>",
                underTest.render(input2)
        );
    }

    @Test
    public void testUnusedMethods() {
        TreeRenderer rowRenderer = underTest.renderers.getRenderer(DokuwikiParser.RowContext.class);
        assertThrows(RuntimeException.class, () -> rowRenderer.render(Mockito.mock(DokuwikiParser.RowContext.class)));
        TreeRenderer codeBoxRenderer = underTest.renderers.getRenderer(DokuwikiParser.Code_boxContext.class);
        assertThrows(RuntimeException.class, () -> codeBoxRenderer.render(Mockito.mock(DokuwikiParser.Code_boxContext.class)));
    }
}