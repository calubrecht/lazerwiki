package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = { DokuWikiRenderer.class, RendererRegistrar.class, DokuWikiRendererTest.TestConfig.class})
@ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
@ActiveProfiles("test")
public class DokuWikiRendererTest {

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
        return underTest.renderToString(source, "localhost", "default", "");
    }

    @Test
    void testRenderHeader() {
        String source = "====== Big header ======\n ==== Smaller Header ====";

        assertEquals("<h1>Big header</h1>\n<h3>Smaller Header</h3>", doRender(source));

        assertEquals("<h2>Header with space.</h2>", doRender("=====Header with space.===== "));

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
        assertEquals("<h1>Some text in <a class=\"wikiLinkMissing\" href=\"/page/headerLink\">a header</a></h1>", doRender("======Some text in [[ headerLink |a header]]======"));

        // Link with dash in URL
        assertEquals("<h1>Some text in <a class=\"wikiLinkMissing\" href=\"/page/headerLink\">a header</a></h1>", doRender("======Some text in [[ headerLink |a header]]======"));
        assertEquals("<div><a class=\"wikiLinkExternal\" href=\"http://domain.example/a-page\">http://domain.example/a-page</a></div>", doRender("[[http://domain.example/a-page]]"));


        // broken link syntax
        assertEquals("<div>[[not quite a link]</div>", doRender("[[not quite a link]"));
    }

    @Test
    public void testRenderLinkSanitizesLinks() {
        String linkEmbeddingJS = "[[what \" onclick=\"doEvil|This link may be evil]]";
        assertEquals("<div><a class=\"wikiLinkMissing\" href=\"/page/what_onclick_doEvil\">This link may be evil</a></div>", doRender(linkEmbeddingJS));

        // External Link
        assertEquals("<div>[[https://ListGoesWhere\" onclick=\"evil</div>", doRender("[[https://ListGoesWhere\" onclick=\"evil"));

    }

    @Test
    public void testRenderLinkToHome() {
        when(pageService.getTitle(eq("localhost"), eq(""))).thenReturn("Home");
        when(pageService.exists(eq("localhost"), eq(""))).thenReturn(true);
        assertEquals("<div><a class=\"wikiLink\" href=\"/\">Home</a></div>", doRender("[[]]"));
        assertEquals("<div><a class=\"wikiLink\" href=\"/\">Name of Home</a></div>", doRender("[[|Name of Home]]"));
    }

    @Test
    public void testRenderLinkOtherSite() {
        when(pageService.exists(eq("otherHost"), eq("exists"))).thenReturn(true);
        assertEquals("<div><a class=\"wikiLink\" href=\"/page/exists\">This link exists</a></div>", underTest.renderToString("[[exists|This link exists]]", "otherHost", "default", ""));

    }

    @Test
    public void testRenderMalformedURL() {
        assertEquals("<div><a class=\"wikiLinkExternal\" href=\"http://malformed.invalid\">http://malformed.invalid</a></div>", doRender("[[http://bad%link]]"));

    }

    @Test
    public void testRenderSanitizeHtmlInText() {
        assertEquals("<div>This &lt;b&gt;source&lt;/b&gt; has markup and &lt;script&gt;console.log(\"hey buddy\");&lt;/script&gt;</div>", doRender("This <b>source</b> has markup and <script>console.log(\"hey buddy\");</script>"));

        assertEquals("<div>Escape &lt;b&gt;this&lt;/b&gt; but not <a class=\"wikiLinkMissing\" href=\"/page/aLink\"> a link</a> and &lt;b&gt;escape&lt;/b&gt; again</div>", doRender("Escape <b>this</b> but not [[ aLink | a link]] and <b>escape</b> again"));

    }

    @Test
    public void testCanGetDefaultRendererForUnknownClass() {
        assertEquals(null, underTest.renderers.getRenderer(Integer.class).getTargets());
    }

    @Test
    public void testLinebreaks() {
        String input1 = "A single linebreak in the source\nwill not break in the output";
        assertEquals("<div>A single linebreak in the source\nwill not break in the output</div>", doRender(input1));

        String input2 = "A double linebreak in the source\n\nbreaks in to paragraphs";
        assertEquals("<div>A double linebreak in the source</div>\n<div>breaks in to paragraphs</div>", doRender(input2));
    }

    @Test
    public void testRenderBold() {
        String input1 = "Some words are **meant to **be bold.";
        assertEquals("<div>Some words are <span class=\"bold\">meant to </span>be bold.</div>", doRender(input1));

        String input2 = "Some bolds **have [[link|links]] **";
        assertEquals("<div>Some bolds <span class=\"bold\">have <a class=\"wikiLinkMissing\" href=\"/page/link\">links</a> </span></div>", doRender(input2));

        String input3 = "Some bolds **aren't matched";
        assertEquals("<div>Some bolds **aren't matched</div>", doRender(input3));
        String input4 = "Can **bold\nspan lines?**";
        assertEquals("<div>Can <span class=\"bold\">bold\nspan lines?</span></div>", doRender(input4));
    }

    @Test
    public void testRenderItalic() {
        String input1 = "Some words are //meant to //be italic.";
        assertEquals("<div>Some words are <span class=\"italic\">meant to </span>be italic.</div>", doRender(input1));

        String input2 = "Some italics //have [[link|links]] //";
        assertEquals("<div>Some italics <span class=\"italic\">have <a class=\"wikiLinkMissing\" href=\"/page/link\">links</a> </span></div>", doRender(input2));

        String input3 = "Some italics //aren't matched";
        assertEquals("<div>Some italics //aren't matched</div>", doRender(input3));
        String input4 = "Can //italic\nspan lines?//";
        assertEquals("<div>Can <span class=\"italic\">italic\nspan lines?</span></div>", doRender(input4));
        String input5 = "Can **//italic be in// bold**?";
        assertEquals("<div>Can <span class=\"bold\"><span class=\"italic\">italic be in</span> bold</span>?</div>", doRender(input5));
        String input6 = "Can //**bold be in** italic//?";
        assertEquals("<div>Can <span class=\"italic\"><span class=\"bold\">bold be in</span> italic</span>?</div>", doRender(input6));
    }

    @Test
    public void testRenderUnderline() {
        String input1 = "__This__ should be underlined.";
        assertEquals("<div><span class=\"underline\">This</span> should be underlined.</div>", doRender(input1));
        String input2 = "__This [[underline|under line]]__ should have a link";
        assertEquals("<div><span class=\"underline\">This <a class=\"wikiLinkMissing\" href=\"/page/underline\">under line</a></span> should have a link</div>", doRender(input2));

        String input3 = "Some underlines __aren't matched";
        assertEquals("<div>Some underlines __aren't matched</div>", doRender(input3));
        String input4 = "Can __underlines\nspan lines?__";
        assertEquals("<div>Can <span class=\"underline\">underlines\nspan lines?</span></div>", doRender(input4));
        String input5 = "Can **__underline be in__ bold**?";
        assertEquals("<div>Can <span class=\"bold\"><span class=\"underline\">underline be in</span> bold</span>?</div>", doRender(input5));
        String input6 = "Can __**bold be in** underline__?";
        assertEquals("<div>Can <span class=\"underline\"><span class=\"bold\">bold be in</span> underline</span>?</div>", doRender(input6));
    }

    @Test
    public void testRenderMonospace() {
        String input1 = "''This'' should be monospace.";
        assertEquals("<div><span class=\"monospace\">This</span> should be monospace.</div>", doRender(input1));
        String input2 = "''This [[monospace|monospace]]'' should have a link";
        assertEquals("<div><span class=\"monospace\">This <a class=\"wikiLinkMissing\" href=\"/page/monospace\">monospace</a></span> should have a link</div>", doRender(input2));

        String input3 = "Some monospace ''aren't matched";
        assertEquals("<div>Some monospace ''aren't matched</div>", doRender(input3));
        String input4 = "Can ''monospace\nspan lines?''";
        assertEquals("<div>Can <span class=\"monospace\">monospace\nspan lines?</span></div>", doRender(input4));
        String input5 = "Can **''monospace be in'' bold**?";
        assertEquals("<div>Can <span class=\"bold\"><span class=\"monospace\">monospace be in</span> bold</span>?</div>", doRender(input5));
        String input6 = "Can ''**bold be in** monospace''?";
        assertEquals("<div>Can <span class=\"monospace\"><span class=\"bold\">bold be in</span> monospace</span>?</div>", doRender(input6));
    }

    public void testRenderSuperSubDel() {
        String input1 = "<sup>This</sup> should be superscript.";
        assertEquals("<div><sup>This</sup> should be superscript.</div>", doRender(input1));
        String input2 = "<sub>This</sub> should be subscript.";
        assertEquals("<div><sub>This</sub> should be subscript.</div>", doRender(input2));
        String input3 = "<del>This</del> should be deleted.";
        assertEquals("<div><del>This</del> should be deleted.</div>", doRender(input3));

        String input4 = "Can <sup>monospace\nspan</sup> lines?''";
        assertEquals("<div>Can <sup>monospace\nspan lines?</sup></div>", doRender(input4));


    }

    @Test
    public void testRenderImage() {
        String input1 = "{{img.jpg}}";
        assertEquals(
                "<div><img src=\"/_media/img.jpg\" class=\"media\" loading=\"lazy\"></div>",
                doRender(input1)
        );

        // Image inside link
        String input2 = "[[somePage|w {{img.jpg}} y]]";
        assertEquals(
                "<div><a class=\"wikiLinkMissing\" href=\"/page/somePage\">w <img src=\"/_media/img.jpg\" class=\"media\" loading=\"lazy\"> y</a></div>",
                doRender(input2)
        );

        // Image with dash in name
        String input3 = "{{an-image.jpg}}";
        assertEquals(
                "<div><img src=\"/_media/an-image.jpg\" class=\"media\" loading=\"lazy\"></div>",
                doRender(input3)
        );

        String inputWithTitle = "{{image.jpg|A title }}";
        assertEquals(
                "<div><img src=\"/_media/image.jpg\" class=\"media\" title=\"A title\" loading=\"lazy\"></div>",
                doRender(inputWithTitle)
        );

        String inputWithSize = "{{image.jpg?10}}";
        assertEquals(
                "<div><img src=\"/_media/image.jpg?10\" class=\"media\" loading=\"lazy\"></div>",
                doRender(inputWithSize)
                );
        String inputWithSizeAndLinkType = "{{image.jpg?nolink&10}}";
        assertEquals(
                "<div><img src=\"/_media/image.jpg?10\" class=\"media\" loading=\"lazy\"></div>",
                doRender(inputWithSizeAndLinkType)
        );

        String inputWithTypeNoSize = "{{image.jpg?nolink}}";
        assertEquals(
                "<div><img src=\"/_media/image.jpg\" class=\"media\" loading=\"lazy\"></div>",
                doRender(inputWithTypeNoSize)
        );
    }


    @Test
    public void testRenderWeirdImage() {
        String linkEmbeddingJS = "{{ thisLinkHastooMany&&options}}";
        assertEquals("<div><img src=\"/_media/thisLinkHastooMany&&options\" class=\"mediaright\" loading=\"lazy\"></div>", doRender(linkEmbeddingJS));

    }

    @Test
    public void testRenderUList() {
        String input1 = " * Simple List\n *With 2 rows\nThen * non-matching\n";
        assertEquals(
                "<div><ul>\n<li>Simple List</li>\n<li>With 2 rows</li>\n</ul>\nThen * non-matching</div>",
                doRender(input1)
        );

        // List after blank line
        String input2 = "Something\n\n * Simple List\n *With 2 rows\nThen * non-matching\n";
        assertEquals(
                "<div>Something</div>\n<div><ul>\n<li>Simple List</li>\n<li>With 2 rows</li>\n</ul>\nThen * non-matching</div>",
                doRender(input2)
        );
    }

    @Test
    public void testRenderOList() {
        String input1 = " - Simple List\n -With 2 rows\nThen * non-matching\n";
        assertEquals(
                "<div><ol>\n<li>Simple List</li>\n<li>With 2 rows</li>\n</ol>\nThen * non-matching</div>",
                doRender(input1)
        );
    }

    @Test
    public void testRenderNestedLists() {
        String input1 = " - Simple List\n  -Deeper List\n   * DeepestList\n";
        assertEquals(
                "<div><ol>\n<li>Simple List</li>\n<ol>\n<li>Deeper List</li>\n<ul>\n<li>DeepestList</li>\n</ul>\n</ol>" +
                        "\n</ol></div>",
                doRender(input1)
        );

        String input2 = " - Simple List\n *List Changes Type\n   * DeepestList\n * and backout\n";
        assertEquals(
                "<div><ol>\n<li>Simple List</li>\n</ol>\n<ul>\n<li>List Changes Type</li>\n<ul>\n<li>DeepestList</li>\n" +
                        "</ul>\n<li>and backout</li>\n</ul></div>",
                doRender(input2)
        );
    }

    @Test
    public void testCodeBlock() {
        String input1 = "  This is a block\n  Should all be one block\n   with more spaces?\n";
        assertEquals(
                "<pre class=\"code\">This is a block\nShould all be one block\n with more spaces?\n</pre>",
                doRender(input1)
        );

        String input2 = "**bold on one line**\n  Raw text box, do not render **bold things**\n";
        assertEquals(
                "<div><span class=\"bold\">bold on one line</span></div><pre class=\"code\">Raw text box, do not render **bold things**\n</pre>",
                doRender(input2)
        );
    }

    @Test
    public void testHeaderInBox() {
        String input1 = "  This is a block\n  ==== It has a header in it ====\n";
        assertEquals(
                "<pre class=\"code\">This is a block\n==== It has a header in it ====\n</pre>",
                doRender(input1)
        );

    }

    @Test
    public void testUnusedMethods() {
        TreeRenderer rowRenderer = underTest.renderers.getRenderer(DokuwikiParser.RowContext.class);
        assertThrows(RuntimeException.class, () -> rowRenderer.render(Mockito.mock(DokuwikiParser.RowContext.class), new RenderContext("localhost", "default", "")));
        TreeRenderer codeBoxRenderer = underTest.renderers.getRenderer(DokuwikiParser.Code_boxContext.class);
        assertThrows(RuntimeException.class, () -> codeBoxRenderer.render(Mockito.mock(DokuwikiParser.Code_boxContext.class), new RenderContext("localhost","default", "")));
    }

    @Test
    public void testRenderTitles() {
        String input1 = "=== Here's a title===\n";
        RenderResult result = underTest.renderWithInfo(input1, "host", "site", "");
        assertEquals("Here's a title", result.getTitle());
        String input2 = "==== A title [[WithSomeLink|With Some Link]] ====\n";
        result = underTest.renderWithInfo(input2, "host", "site", "");
        assertEquals("A title With Some Link", result.getTitle());

        String input3 = "Title may not be on first line\n== But you'll find it==\n";
        result = underTest.renderWithInfo(input3, "host", "site", "");
        assertEquals("But you'll find it", result.getTitle());

        String input4 = "This has no title\n";
        result = underTest.renderWithInfo(input4, "host", "site", "");
        assertEquals(null, result.getTitle());

        String input5 = "=== This is the title===\n==== This is just another header ====\n";
        result = underTest.renderWithInfo(input5, "host", "site", "");
        assertEquals("This is the title", result.getTitle());
    }

    @Test
    public void testRenderMacro() {
        String inputMacro = "~~MACRO~~macro1: macro~~/MACRO~~";
        when(macroService.renderMacro(eq("macro1: macro"), any())).thenReturn("<div>MACRO- Unknown Macro macro1</div>");
        String render = underTest.renderToString(inputMacro, "", "", "");
        assertEquals("<div><div>MACRO- Unknown Macro macro1</div></div>", render);


    }

    @Test
    public void testRenderWithContext() {
        RenderContext context = new RenderContext("site", "localhost", "user");
        context.renderState().put("rememberedState", "State");
        RenderResult res = underTest.renderWithInfo("===Some Header===", context);
        assertEquals("Some Header", res.renderState().get(RenderResult.RENDER_STATE_KEYS.TITLE.name()));
        // State sent in to render should remain when returned
        assertEquals("State", res.renderState().get("rememberedState"));
    }
}