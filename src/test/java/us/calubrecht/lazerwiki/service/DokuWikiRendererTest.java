package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.LinkOverride;
import us.calubrecht.lazerwiki.model.LinkOverrideInstance;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.nodes.LinkNode;
import us.calubrecht.lazerwiki.syntax.renderer.LinkRenderer;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.OVERRIDE_STATS;

@SuppressWarnings({"unchecked", "HttpUrlsUsage"})
@SpringBootTest(classes = { CustomWikiRenderer.class, DokuWikiRendererTest.TestConfig.class})
@ActiveProfiles("test")
public class DokuWikiRendererTest {
    @Autowired
    CustomWikiRenderer underTest;

    @Configuration
    @ComponentScan({"us.calubrecht.lazerwiki.syntax"})
    public static class TestConfig {
    }

    @MockitoBean
    PageService pageService;

    @MockitoBean
    MacroService macroService;

    @MockitoBean
    RandomService randomService;

    @MockitoBean
    LinkOverrideService linkOverrideService;

    @MockitoBean
    MediaOverrideService mediaOverrideService;

    @MockitoBean
    TOCRenderService tocRenderService;

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
    void testRenderHeaderBlanBetween() {
        String source = "====== Big header ======\n\n ==== Smaller Header ====";

        assertEquals("<h1 id=\"header_Big_header\">Big header</h1>\n<h3 id=\"header_Smaller_Header\">Smaller Header</h3>", doRender(source));
    }

    @Test
    void testRenderHeaderImbalance() {
        // Imbalanced header tokens? Just use the opening tokens for size
        String source = "====== Header with unmatching tokens =====";
        assertEquals("<h1 id=\"header_Header_with_unmatching_tokens\">Header with unmatching tokens</h1>", doRender(source));
        source = "==== Mismatched the other way =====";
        assertEquals("<h3 id=\"header_Mismatched_the_other_way\">Mismatched the other way</h3>", doRender(source));

        source = "= Not enough token =====";
        assertEquals("<div>= Not enough token =====</div>", doRender(source));


        source = "==== Header == Text After";
        assertEquals("<div>==== Header == Text After</div>", doRender(source));

        source = "==";
        assertEquals("<div>==</div>", doRender(source));
    }

    @Test
    void testRenderHeaderSanitize() {
        String source = "====== <script>doAlert(\"Gotcha\");</script> ======";

        assertEquals("<h1 id=\"header__script_doAlert__Gotcha_____script_\">&lt;script&gt;doAlert(\"Gotcha\");&lt;/script&gt;</h1>", doRender(source));
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

    @Test
    public void testRenderLinkSanitizesLinks() {
        String linkEmbeddingJS = "[[what \" onclick=\"doEvil|This link may be evil]]";
        assertEquals("<div>[[what \" onclick=\"doEvil|This link may be evil]]</div>", doRender(linkEmbeddingJS)); // quotes force link parsing to fail

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
        assertEquals("<div><a class=\"wikiLink\" href=\"/page/exists\">This link exists</a></div>", underTest.renderToString("[[exists|This link exists]]", "otherHost", "default", "",""));
    }

    @Test
    public void testRenderLinkRecordsLinks() {
        when(pageService.getTitle(anyString(), anyString())).thenReturn("");
        RenderResult result = underTest.renderWithInfo("[[oneLink]]\n[[oneLinkWithText|The text]] [[http://external.link]] \n[[ns:ThirdLink]]", "host", "site","page","user");
        Set<String> links = (Set<String>)result.renderState().get(RenderResult.RENDER_STATE_KEYS.LINKS.name());
        assertEquals(Set.of("oneLink", "oneLinkWithText", "ns:ThirdLink"), links);
    }

    @Test
    public void testRenderLinkWithOverrides() {
        when(pageService.getTitle(anyString(), eq("new"))).thenReturn("new");
        when(pageService.getTitle(anyString(), eq("ns2:wns2"))).thenReturn("with ns");
        when(pageService.exists(eq("otherHost"), eq("new"))).thenReturn(true);
        when(pageService.exists(eq("otherHost"), eq("ns2:wns2"))).thenReturn(true);
        List<LinkOverride> overrides = List.of(
                new LinkOverride("default","", "source", "", "overridden", "", "old"),
                new LinkOverride("default","", "source", "", "overridden", "", "new"),
                new LinkOverride("default","", "source", "ns1", "wns", "ns2", "wns2")
        );
        when(linkOverrideService.getOverrides(anyString(), anyString())).thenReturn(overrides);
        RenderContext context = new RenderContext("otherHost", "default", "page", "");
        String source = "[[overridden]] [[ns1:wns|wtitle]]";
        assertEquals("<div><a class=\"wikiLink\" href=\"/page/new\">new</a> <a class=\"wikiLink\" href=\"/page/ns2:wns2\">wtitle</a></div>", underTest.renderToString(source, context));
        List<LinkOverrideInstance> overrideInstances = (List<LinkOverrideInstance>)context.renderState().get(OVERRIDE_STATS.name());
        assertEquals(2, overrideInstances.size());
        LinkOverrideInstance o1 =overrideInstances.get(0);
        LinkOverrideInstance o2 =overrideInstances.get(1);
        StringBuilder sb = new StringBuilder(source);
        sb.replace(o2.start(), o2.stop(), o2.override());
        sb.replace(o1.start(), o1.stop(), o1.override());
        String fixedSource = sb.toString();
        assertEquals("[[new]] [[ns2:wns2|wtitle]]", fixedSource);

        // Try w/ multiline doc
        context = new RenderContext("otherHost", "default", "page", "");
        String source2 = "AnotherParagraph\n\n[[overridden]] [[ns1:wns|wtitle]]";
        assertEquals("<div>AnotherParagraph</div>\n<div><a class=\"wikiLink\" href=\"/page/new\">new</a> <a class=\"wikiLink\" href=\"/page/ns2:wns2\">wtitle</a></div>", underTest.renderToString(source2, context));
        overrideInstances = (List<LinkOverrideInstance>)context.renderState().get(OVERRIDE_STATS.name());
        assertEquals(2, overrideInstances.size());
        o1 =overrideInstances.get(0);
        o2 =overrideInstances.get(1);
        sb = new StringBuilder(source2);
        sb.replace(o2.start(), o2.stop(), o2.override());
        sb.replace(o1.start(), o1.stop(), o1.override());
        fixedSource = sb.toString();
        assertEquals("AnotherParagraph\n\n[[new]] [[ns2:wns2|wtitle]]", fixedSource);

    }

    @Autowired
    LinkRenderer linkRenderer;
    @Test
    public void testRenderLinkeSanitize() {
        String maliciousText = "[[ns:page|<script>someScript</script>]]";
        RenderResult renderRes = underTest.renderWithInfo(maliciousText, "host", "site", "page", "user");
        assertEquals("<div><a class=\"wikiLinkMissing\" href=\"/page/ns:page\">&lt;script&gt;someScript&lt;/script&gt;</a></div>", renderRes.renderedText());
        List<String> parseErrors = (List<String>) renderRes.renderState().get(RenderResult.RENDER_STATE_KEYS.ERRORS.name());
        assertEquals("Suspicious link source at 0. Raw text =[[[ns:page|<script>someScript</script>]]]", parseErrors.get(0));

        String maliciousHref = "[[ page\" onerror=\"alert(1)\"| text}}";
        renderRes = underTest.renderWithInfo(maliciousHref, "host", "site", "page", "user");
        assertEquals("<div>[[ page\" onerror=\"alert(1)\"| text}}</div>", renderRes.renderedText()); // Fails at parsing level, outputs safe html

        // If maliicous code slips through parser to node, sanitize anyway.
        LinkNode badNode = new LinkNode("page\" onerror=\"alert(1)\"");
        badNode.setParseContext(new ParseContext("[[page\" onerror=\"alert(1)\"]]"));
        badNode.setPosition(0, 10);
        RenderContext renderContext = new RenderContext("host", "site", "page", "user");
        String html = linkRenderer.renderHtml(badNode, renderContext).toString();
        assertEquals("<a class=\"wikiLinkMissing\" href=\"/page/none:invalidPage\">null</a>", html);

        String maliciousProtocol = "[[javascript:ortext| text]]";
        renderRes = underTest.renderWithInfo(maliciousProtocol, "host", "site", "page", "user");
        assertEquals("<div><a class=\"wikiLinkMissing\" href=\"/page/javascript:ortext\"> text</a></div>", renderRes.renderedText());
        // this is a valid possible image, but suspicious. Render it safely, but log it.
    }

    @Test
    public void testRenderMalformedURL() {
        assertEquals("<div>[[http://bad%link]]</div>", doRender("[[http://bad%link]]"));
        assertEquals("<div><a class=\"wikiLinkExternal\" href=\"http://malformed.invalid\">http://malformed.invalid</a></div>", doRender("[[http://]]"));

    }

    @Test
    public void testRenderSanitizeHtmlInText() {
        String sourcetoSanitize = "This <b>source</b> has markup and <script>console.log(\"hey buddy\");</script>";
        RenderResult renderRes = underTest.renderWithInfo(sourcetoSanitize, "host", "site", "page", "user");
        assertEquals("<div>This &lt;b&gt;source&lt;/b&gt; has markup and &lt;script&gt;console.log(\"hey buddy\");&lt;/script&gt;</div>", renderRes.renderedText());
        List<String> parseErrors = (List<String>) renderRes.renderState().get(RenderResult.RENDER_STATE_KEYS.ERRORS.name());
        assertEquals("Suspicious text at 0. Raw text =[This <b>source</b> has markup and <script>console.log(\"hey buddy\");</script>]", parseErrors.get(0));

        assertEquals("<div>Escape &lt;b&gt;this&lt;/b&gt; but not <a class=\"wikiLinkMissing\" href=\"/page/aLink\"> a link</a> and &lt;b&gt;escape&lt;/b&gt; again</div>", doRender("Escape <b>this</b> but not [[ aLink | a link]] and <b>escape</b> again"));
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
    public void testRenderUnformat() {
        String input1 = "%%This **should not be bold**%%";
        assertEquals("<div>This **should not be bold**</div>", doRender(input1));
        String input2 = "%%This [[underline|under line]]__ should not have a link%%";
        assertEquals("<div>This [[underline|under line]]__ should not have a link</div>", doRender(input2));

        String input3 = "Some unformats %%aren't matched";
        assertEquals("<div>Some unformats %%aren't matched</div>", doRender(input3));
        String input4 = "Can %%unformats\nspan lines?%%";
        assertEquals("<div>Can unformats\nspan lines?</div>", doRender(input4));
        String input5 = "Can **%% be in%% bold**?";
        assertEquals("<div>Can <span class=\"bold\"> be in bold</span>?</div>", doRender(input5));
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
    @Test
    public void testRenderSuperSubDel() {
        String input1 = "<sup>This</sup> should be superscript.";
        assertEquals("<div><sup>This</sup> should be superscript.</div>", doRender(input1));
        String input2 = "<sub>This</sub> should be subscript.";
        assertEquals("<div><sub>This</sub> should be subscript.</div>", doRender(input2));
        String input3 = "<del>This</del> should be deleted.";
        assertEquals("<div><del>This</del> should be deleted.</div>", doRender(input3));

        String input4 = "Can <sup>super\nspan</sup> lines?";
        assertEquals("<div>Can <sup>super\nspan</sup> lines?</div>", doRender(input4));

        String input5 = "<super>What is an unknownTag?</super>";
        assertEquals("<div>&lt;super&gt;What is an unknownTag?&lt;/super&gt;</div>", doRender(input5));
    }

    @Test
    public void testRenderUnknownSpan() {
        String input1 = "<unknown>What is this tag?</unknown>";
        assertEquals("<div>&lt;unknown&gt;What is this tag?&lt;/unknown&gt;</div>", doRender(input1));
        String input2 = "<sub>This one is mispatched</sup>";
        assertEquals("<div>&lt;sub&gt;This one is mispatched&lt;/sup&gt;</div>", doRender(input2));
    }

    @Test
    public void testRenderSpanStartsSpace() {
        String input = " __//**UnderItaliBold **//__";
        assertEquals("<div> <span class=\"underline\"><span class=\"italic\"><span class=\"bold\">UnderItaliBold </span></span></span></div>", doRender(input));
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

        String inputWithTypeFullLink = "{{image.jpg?fullLink}}";
        assertEquals(
                "<div><img src=\"/_media/image.jpg\" class=\"media fullLink\" loading=\"lazy\"></div>",
                doRender(inputWithTypeFullLink)
        );

        String inputWithLinkOnly = "{{image.jpg?linkonly}}";
        assertEquals(
                "<div><a href=\"/_media/image.jpg\" class=\"media linkOnly\" target=\"_blank\">image.jpg</a></div>",
                doRender(inputWithLinkOnly)
        );
        String inputWithLinkOnlyAndName = "{{image.jpg?linkonly|LinkName}}";
        assertEquals(
                "<div><a href=\"/_media/image.jpg\" class=\"media linkOnly\" target=\"_blank\">LinkName</a></div>",
                doRender(inputWithLinkOnlyAndName)
        );

        String inputWithBrokenImagetag = "{{image.jpg";
        assertEquals(
                "<div>{{image.jpg</div>",
                doRender(inputWithBrokenImagetag)
        );
    }

    @Test
    public void testRenderImageAlignments() {
        String input1 = "{{img.jpg}}";
        assertEquals(
                "<div><img src=\"/_media/img.jpg\" class=\"media\" loading=\"lazy\"></div>",
                doRender(input1)
        );

        String leftImage = "{{img.jpg }}";
        assertEquals(
                "<div><img src=\"/_media/img.jpg\" class=\"medialeft\" loading=\"lazy\"></div>",
                doRender(leftImage)
        );

        String rightImage = "{{ img.jpg}}";
        assertEquals(
                "<div><img src=\"/_media/img.jpg\" class=\"mediaright\" loading=\"lazy\"></div>",
                doRender(rightImage)
        );

        String centerImage = "{{ img.jpg }}";
        assertEquals(
                "<div><img src=\"/_media/img.jpg\" class=\"mediacenter\" loading=\"lazy\"></div>",
                doRender(centerImage)
        );
    }

    @Test
    public void testRenderWeirdImage() {
        String linkEmbeddingJS = "{{ thisLinkHastooMany&&options}}";
        assertEquals("<div><img src=\"/_media/thisLinkHastooMany&&options\" class=\"mediaright\" loading=\"lazy\"></div>", doRender(linkEmbeddingJS));

    }

    @Test
    public void testRenderImageRecordsRefs() {
        String imageInput = "{{image.jpg}}";
        RenderResult renderRes = underTest.renderWithInfo(imageInput, "host", "site", "page", "user");
        assertEquals(Set.of("image.jpg"), renderRes.renderState().get(RenderResult.RENDER_STATE_KEYS.IMAGES.name()));
        String linkOnlyInput = "{{image.jpg?linkonly}}";
        renderRes = underTest.renderWithInfo(linkOnlyInput, "host", "site", "page", "user");
        assertEquals(Set.of("image.jpg"), renderRes.renderState().get(RenderResult.RENDER_STATE_KEYS.IMAGES.name()));
    }

    @Test
    public void testRenderImageSanitize() {
        String maliciousTitle = "Check {{file.jpg|\" onerror=\"alert(1)\"}}";
        RenderResult renderRes = underTest.renderWithInfo(maliciousTitle, "host", "site", "page", "user");
        assertEquals("<div>Check <img src=\"/_media/file.jpg\" class=\"media\" title=\"&quot; onerror=&quot;alert(1)&quot;\" loading=\"lazy\"></div>", renderRes.renderedText());
        List<String> parseErrors = (List<String>) renderRes.renderState().get(RenderResult.RENDER_STATE_KEYS.ERRORS.name());
        assertEquals("Suspicious img tag title at 6. Raw text =[\" onerror=\"alert(1)\"]", parseErrors.get(0));

        String maliciousSource = "Check {{\" onerror=\"alert(1)\"| text}}";
        renderRes = underTest.renderWithInfo(maliciousSource, "host", "site", "page", "user");
        assertEquals("<div>Check <img src=\"/_media/invalidSource.none\" class=\"media\" title=\"text\" loading=\"lazy\"></div>", renderRes.renderedText());
        parseErrors = (List<String>) renderRes.renderState().get(RenderResult.RENDER_STATE_KEYS.ERRORS.name());
        assertEquals("Suspicious img tag src at 6. Raw text =[\" onerror=\"alert(1)\"]", parseErrors.get(0));

        String maliciousProtocol = "{{javascript:ortext| text}}";
        renderRes = underTest.renderWithInfo(maliciousProtocol, "host", "site", "page", "user");
        assertEquals("<div><img src=\"/_media/javascript:ortext\" class=\"media\" title=\"text\" loading=\"lazy\"></div>", renderRes.renderedText());
        parseErrors = (List<String>) renderRes.renderState().get(RenderResult.RENDER_STATE_KEYS.ERRORS.name());
        assertEquals("Suspicious img tag src at 0. Raw text =[javascript:ortext]", parseErrors.get(0));
    }

    @Test
    public void testRenderUList() {
        String input1 = " * Simple List\n *With 2 rows\nThen * non-matching\n";
        assertEquals(
                "<ul>\n<li>Simple List</li>\n<li>With 2 rows</li>\n</ul>\n<div>Then * non-matching</div>",
                doRender(input1)
        );

        // List after blank line
        String input2 = "Something\n\n * Simple List\n *With 2 rows\nThen * non-matching\n";
        assertEquals(
                "<div>Something</div>\n<ul>\n<li>Simple List</li>\n<li>With 2 rows</li>\n</ul>\n<div>Then * non-matching</div>",
                doRender(input2)
        );

        // List item with bold
        // names on a list item are accepted, but ignored.
        String inputBold = """
                 * **item1**
                 *{{5}} **item2** - is mixed
                """;
        assertEquals(
                "<ul>\n<li><span class=\"bold\">item1</span></li>\n<li><span class=\"bold\">item2</span> - is mixed</li>\n</ul>",
                doRender(inputBold)
        );
    }

    @Test
    public void testRenderUList_startsDeeper() {
        String input1 = "  * Simple List\n  *With 2 rows\n";
        assertEquals(
                "<ul>\n<li>Simple List</li>\n<li>With 2 rows</li>\n</ul>",
                doRender(input1)
        );
    }

    @Test
    public void testRenderOList() {
        String input1 = " - Simple List\n -With 2 rows\nThen * non-matching\n";
        assertEquals(
                "<ol>\n<li>Simple List</li>\n<li>With 2 rows</li>\n</ol>\n<div>Then * non-matching</div>",
                doRender(input1)
        );
    }

    @Test
    public void testRenderOListWithValues() {
        String input1 = " - Simple List\n -{{5}}With one row value defined\n -One follow\n";
        assertEquals(
                "<ol>\n<li>Simple List</li>\n<li value=\"5\">With one row value defined</li>\n<li>One follow</li>\n</ol>",
                doRender(input1)
        );
    }

    @Test
    public void testRenderNestedLists() {
        String input1 = " - Simple List\n  -Deeper List\n   * DeepestList\n";
        assertEquals(
                """
                        <ol>
                        <li>Simple List</li>
                        <ol>
                        <li>Deeper List</li>
                        <ul>
                        <li>DeepestList</li>
                        </ul>
                        </ol>
                        </ol>""",
                doRender(input1)
        );

        String input2 = " - Simple List\n *List Changes Type\n   * DeepestList\n * and backout\n";
        assertEquals(
                """
                        <ol>
                        <li>Simple List</li>
                        </ol>
                        <ul>
                        <li>List Changes Type</li>
                        <ul>
                        <li>DeepestList</li>
                        </ul>
                        <li>and backout</li>
                        </ul>""",
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
                "<div><span class=\"bold\">bold on one line</span></div>\n<pre class=\"code\">Raw text box, do not render **bold things**\n</pre>",
                doRender(input2)
        );

        String input3 = "  This is a block\n  Should all be one block\n  \n  Even if a line with only 2 spaces and nothing more\n";
        assertEquals(
                "<pre class=\"code\">This is a block\nShould all be one block\n\nEven if a line with only 2 spaces and nothing more\n</pre>",
                doRender(input3)
        );
    }

    @Test
    public void testCodeBlockThenSpace() {
        String input1 = "  This is a block\n\n  andAnotherBlock\n";
        assertEquals(
                "<pre class=\"code\">This is a block\n</pre><pre class=\"code\">andAnotherBlock\n</pre>",
                doRender(input1)
        );
    }

    @Test
    public void testCodeBlockSanitize() {
        String input1 = "  This is a block <script>doAlert(\"Gotcha\");</script>";
        assertEquals(
                "<pre class=\"code\">This is a block &lt;script&gt;doAlert(\"Gotcha\");&lt;/script&gt;\n" +
                        "</pre>",
                doRender(input1)
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
    public void testRenderTitles() {
        String input1 = "=== Here's a title===\n";
        RenderResult result = underTest.renderWithInfo(input1, "host", "site", "page", "");
        assertEquals("Here's a title", result.getTitle());
        String input2 = "==== A title [[WithSomeLink|With Some Link]] ====\n";
        result = underTest.renderWithInfo(input2, "host", "site", "page", "");
        assertEquals("A title With Some Link", result.getTitle());

        String input3 = "Title may not be on first line\n== But you'll find it==\n";
        result = underTest.renderWithInfo(input3, "host", "site", "page", "");
        assertEquals("But you'll find it", result.getTitle());

        String input4 = "This has no title\n";
        result = underTest.renderWithInfo(input4, "host", "site", "page", "");
        assertNull(result.getTitle());

        String input5 = "=== This is the title===\n==== This is just another header ====\n";
        result = underTest.renderWithInfo(input5, "host", "site", "page", "");
        assertEquals("This is the title", result.getTitle());
    }

    @Test
    public void testRenderMacro() {

        String inputMacro = "~~MACRO~~macro1: macro~~/MACRO~~";
        when(macroService.renderMacro(eq("macro1: macro"), anyString(), any())).thenReturn("<div>MACRO- Unknown Macro macro1</div>");
        String render = underTest.renderToString(inputMacro, "", "", "page", "");
        assertEquals("<div>MACRO- Unknown Macro macro1</div>", render);

        when(macroService.renderMacro(Mockito.startsWith("macro1: Start1"), anyString(), any())).thenReturn("<div>Inside Macro 1</div>");
        when(macroService.renderMacro(Mockito.startsWith("macro2"), anyString(), any())).thenReturn("<div>Inside Macro 2</div>");

        // Outside most ~~MACRO~~ and ~~/MACRO~~ tags should be matched and pass entire thing to macroService
        String nestedMacro = "~~MACRO~~macro1: Start1 ~~MACRO~~macro2: Start2 ~~/MACRO~~ End1 ~~/MACRO~~ Outside";
        render = underTest.renderToString(nestedMacro, "", "", "page", "");
        assertEquals("<div>Inside Macro 1</div><div> Outside</div>", render);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(macroService, times(2)).renderMacro(captor.capture(), anyString(), any());

        assertEquals("macro1: Start1 ~~MACRO~~macro2: Start2 ~~/MACRO~~ End1 ", captor.getAllValues().get(1));

        String sideBySideMacro = "~~MACRO~~macro1: Start1 ~~/MACRO~~~~MACRO~~macro2: Start2 ~~/MACRO~~ Outside";
        render = underTest.renderToString(sideBySideMacro, "", "", "page", "");
        assertEquals("<div>Inside Macro 1</div><div>Inside Macro 2</div><div> Outside</div>", render);
        captor = ArgumentCaptor.forClass(String.class);
        verify(macroService, times(4)).renderMacro(captor.capture(), anyString(), any());

        assertEquals("macro1: Start1 ", captor.getAllValues().get(2));
        assertEquals("macro2: Start2 ", captor.getAllValues().get(3));
    }

    @Test
    public void testRenderMultilineMacro() {

        String inputMacro = "~~MACRO~~macro1: macro  \n\n This is line~~/MACRO~~";
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        when(macroService.renderMacro(textCaptor.capture(), anyString(), any())).thenReturn("<div>MACRO- Unknown Macro macro1</div>");
        String render = underTest.renderToString(inputMacro, "", "", "page", "");
        assertEquals("<div>MACRO- Unknown Macro macro1</div>", render);

        assertEquals("macro1: macro  \n\n This is line", textCaptor.getValue());
    }

    @Test
    public void testRenderUnfinishedMacro() {

        String inputMacro = "~~MACRO~~macro1: macro  \n\n This is a line";
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        when(macroService.renderMacro(textCaptor.capture(), anyString(), any())).thenReturn("<div>MACRO- Unknown Macro macro1</div>");
        String render = underTest.renderToString(inputMacro, "", "", "page", "");
        assertEquals("""
                <div>~~MACRO~~macro1: macro  </div>
                <div> This is a line</div>""", render);

        verify(macroService, never()).renderMacro(anyString(), anyString(), any());
    }

    @Test
    public void testRenderLinebreak() {
        String input = "This is a line \\\\ with a linebreak";
        assertEquals("<div>This is a line<br> with a linebreak</div>", doRender(input));
        String inputBreakSymbolInHeader = "====This is \\\\ a header====";
        assertEquals("<h3 id=\"header_This_is__a_header\">This is<br> a header</h3>", doRender(inputBreakSymbolInHeader));

        String inputRequireWS = "This is a line\\\\with a linebreak but no spaces";
        assertEquals("<div>This is a line\\\\with a linebreak but no spaces</div>", doRender(inputRequireWS));
    }

    @Test
    public void testRenderWithContext() {
        RenderContext context = new RenderContext("site", "localhost", "page", "user");
        context.renderState().put("rememberedState", "State");
        RenderResult res = underTest.renderWithInfo("===Some Header===", context);
        assertEquals("Some Header", res.renderState().get(RenderResult.RENDER_STATE_KEYS.TITLE.name()));
        // State sent in to render should remain when returned
        assertEquals("State", res.renderState().get("rememberedState"));
    }

    @Test
    public void testRenderTable() {
        String inputSimpleTable = "|First|Line|\n|Second|Line|";
        assertEquals("<table class=\"lazerTable\"><tbody><tr><td>First</td><td>Line</td></tr>\n<tr><td>Second</td><td>Line</td></tr>\n</tbody></table>", doRender(inputSimpleTable));
        String tableWithHeader = "^Header^Line^\n|Second|Line|";
        assertEquals("<table class=\"lazerTable\"><tbody><tr><th>Header</th><th>Line</th></tr>\n<tr><td>Second</td><td>Line</td></tr>\n</tbody></table>", doRender(tableWithHeader));
        String tableWithMixedHeader = "^Header|Line|\n|Second|Line|";
        assertEquals("<table class=\"lazerTable\"><tbody><tr><th>Header</th><td>Line</td></tr>\n<tr><td>Second</td><td>Line</td></tr>\n</tbody></table>", doRender(tableWithMixedHeader));
        String tableWithColSpan = "^Header|Line|\n|Second||";
        assertEquals("<table class=\"lazerTable\"><tbody><tr><th>Header</th><td>Line</td></tr>\n<tr><td colspan=\"2\">Second</td></tr>\n</tbody></table>", doRender(tableWithColSpan));
        String tableWithImg = "|{{img.jpg}} \\\\ Some text after|";
        assertEquals("<table class=\"lazerTable\"><tbody><tr><td><img src=\"/_media/img.jpg\" class=\"media\" loading=\"lazy\"><br> Some text after</td></tr>\n</tbody></table>", doRender(tableWithImg));
        String tableWithLink = "|[[LinkToSomePage]] \\\\ Some text after|";
        when(pageService.getTitle(eq("localhost"), eq("LinkToSomePage"))).thenReturn("LinkToSomePage");
        assertEquals("<table class=\"lazerTable\"><tbody><tr><td><a class=\"wikiLinkMissing\" href=\"/page/LinkToSomePage\">LinkToSomePage</a><br> Some text after</td></tr>\n" +
                "</tbody></table>", doRender(tableWithLink));

        String biggerTable = "|One|Two|Three|Four|\n|Five|Six|Seven|Eight|";
        assertEquals("""
                <table class="lazerTable"><tbody><tr><td>One</td><td>Two</td><td>Three</td><td>Four</td></tr>
                <tr><td>Five</td><td>Six</td><td>Seven</td><td>Eight</td></tr>
                </tbody></table>""", doRender(biggerTable));
        String tableThatEnds = "|First|Line|\nIs the only line";
        assertEquals("<table class=\"lazerTable\"><tbody><tr><td>First</td><td>Line</td></tr>\n</tbody></table>\n<div>Is the only line</div>", doRender(tableThatEnds));
    }

    @Test
    public void testRenderTableWithRowspan() {
        String tableWithRowSpan = "|One|Two|\n|Three| :: |";
        assertEquals("""
                <table class="lazerTable"><tbody><tr><td>One</td><td rowspan="2">Two</td></tr>
                <tr><td>Three</td></tr>
                </tbody></table>""", doRender(tableWithRowSpan));

        tableWithRowSpan = "|One|Two|\n|Three| :: |\n|Four|::|";
        assertEquals("""
                <table class="lazerTable"><tbody><tr><td>One</td><td rowspan="3">Two</td></tr>
                <tr><td>Three</td></tr>
                <tr><td>Four</td></tr>
                </tbody></table>""", doRender(tableWithRowSpan));

        tableWithRowSpan = "|One|Two|Four|\n|Three| :: |Five|";
        assertEquals("""
                <table class="lazerTable"><tbody><tr><td>One</td><td rowspan="2">Two</td><td>Four</td></tr>
                <tr><td>Three</td><td>Five</td></tr>
                </tbody></table>""", doRender(tableWithRowSpan));

        tableWithRowSpan = "|One|Two||\n|Three| :: |Five|";
        assertEquals("""
                <table class="lazerTable"><tbody><tr><td>One</td><td colspan="2" rowspan="2">Two</td></tr>
                <tr><td>Three</td><td>Five</td></tr>
                </tbody></table>""", doRender(tableWithRowSpan));

        // Rowspan must be raw string
        tableWithRowSpan = "|One|Two||\n|Three|** :: **|Five|";
        assertEquals("""
                <table class="lazerTable"><tbody><tr><td>One</td><td colspan="2">Two</td></tr>
                <tr><td>Three</td><td><span class="bold"> :: </span></td><td>Five</td></tr>
                </tbody></table>""", doRender(tableWithRowSpan));

        // Invalid cases. Do something reasonable rather than break
        tableWithRowSpan = "|One| :: |\n|Three| :: |"; // Spanning element on first row, just add nothing
        assertEquals("""
                <table class="lazerTable"><tbody><tr><td>One</td></tr>
                <tr><td>Three</td></tr>
                </tbody></table>""", doRender(tableWithRowSpan));
        tableWithRowSpan = "|One|Two|\n|Three|Four| :: |"; // Spanning element beyond upper row, skip
        assertEquals("""
                <table class="lazerTable"><tbody><tr><td>One</td><td>Two</td></tr>
                <tr><td>Three</td><td>Four</td></tr>
                </tbody></table>""", doRender(tableWithRowSpan));
    }

    @Test
    public void testRenderTableWithAlignment() {
        String tableWithRowSpan = "|Left | Center |\n| Right|None|";
        assertEquals("""
                <table class="lazerTable"><tbody><tr><td class="tableLeft">Left </td><td class="tableCenter"> Center </td></tr>
                <tr><td class="tableRight"> Right</td><td>None</td></tr>
                </tbody></table>""", doRender(tableWithRowSpan));
        tableWithRowSpan = "|**Left** | **Center** |\n| **Right**|**None**|";
        assertEquals("""
                <table class="lazerTable"><tbody><tr><td class="tableLeft"><span class="bold">Left</span> </td><td class="tableCenter"> <span class="bold">Center</span> </td></tr>
                <tr><td class="tableRight"> <span class="bold">Right</span></td><td><span class="bold">None</span></td></tr>
                </tbody></table>""", doRender(tableWithRowSpan));
    }

    @Test
    public void testRenderTableWithBraces() {
        String inputTable = "|<some thing>|<or else>|";
        assertEquals("<table class=\"lazerTable\"><tbody><tr><td>&lt;some thing&gt;</td><td>&lt;or else&gt;</td></tr>\n</tbody></table>", doRender(inputTable));
    }

    @Test
    public void testRenderTableWithLink() {
        String inputTableWithLink = "|First|Cell [[Link| with a link]]|";
        assertEquals("<table class=\"lazerTable\"><tbody><tr><td>First</td><td>Cell <a class=\"wikiLinkMissing\" href=\"/page/Link\"> with a link</a></td></tr>\n</tbody></table>", doRender(inputTableWithLink));
    }

    @Test
    public void testRender2Tables() {
        String inputSimpleTable = "|First|Line|\n\n|Second|Line|";
        assertEquals("""
                <table class="lazerTable"><tbody><tr><td>First</td><td>Line</td></tr>
                </tbody></table>
                <table class="lazerTable"><tbody><tr><td>Second</td><td>Line</td></tr>
                </tbody></table>""", doRender(inputSimpleTable));
    }

    @Test
    public void testRenderBrokenTable() {
        String inputTable = "|Cell1|Cell2|\n|Cell3|Cell4 doesn't end";
        assertEquals("""
                <table class="lazerTable"><tbody><tr><td>Cell1</td><td>Cell2</td></tr>
                </tbody></table>
                <div>|Cell3|Cell4 doesn't end</div>""", doRender(inputTable));
    }

    @Test
    public void testRenderBlockquote() {
        String inputBlockquote = "> One quote **with some bold**\n>And\n>>Another layer of quote";
        assertEquals("<blockquote> One quote <span class=\"bold\">with some bold</span>\n" +
                "<br>And<blockquote>Another layer of quote</blockquote></blockquote>", doRender(inputBlockquote));

        String inputBlockquoteWithBlankLines = "> **//Some bold//**\n>\n>A blank line\n> \n>And one with just space";
        assertEquals("<blockquote> <span class=\"bold\"><span class=\"italic\">Some bold</span></span>\n<br>\n<br>A blank line\n<br> \n<br>And one with just space</blockquote>", doRender(inputBlockquoteWithBlankLines));

        String inputBlockquoteUpAndDown = ">One Quote\n>>TwoQuote\n>One Quote";
        assertEquals("<blockquote>One Quote<blockquote>TwoQuote</blockquote>One Quote</blockquote>", doRender(inputBlockquoteUpAndDown));

        String inputBlockquoteThenPAragraph = ">One Quote\nNot a quote";
        assertEquals("<blockquote>One Quote</blockquote><div>Not a quote</div>", doRender(inputBlockquoteThenPAragraph));
    }

    @Test
    public void testRenderBlockquoteSpaceIMage() {
        String inputBlockquote = "> {{animage}}";
        assertEquals("<blockquote> <img src=\"/_media/animage\" class=\"media\" loading=\"lazy\"></blockquote>", doRender(inputBlockquote));
    }

    @Test
    public void testHidden() {
        when(randomService.nextInt()).thenReturn(5,8, 7, 11);
        String hidden = "<hidden>simple</hidden>";
        assertEquals("<div class=\"hidden\"><input id=\"hiddenToggle5\" class=\"toggle\" type=\"checkbox\"><label for=\"hiddenToggle5\" class=\"hdn-toggle\">Hidden</label><div class=\"collapsible\">simple</div></div>", doRender(hidden));

        assertEquals("<div class=\"hidden\"><input id=\"hiddenToggle8\" class=\"toggle\" type=\"checkbox\"><label for=\"hiddenToggle8\" class=\"hdn-toggle\">Hidden</label><div class=\"collapsible\"><div>line1</div>\n<div>line2<img src=\"/_media/animage\" class=\"media\" loading=\"lazy\"></div></div></div>",
                doRender("<hidden>line1\n\nline2{{animage}}</hidden>"));

        String namedHidden = "<hidden name=\"Bark\">Something in  here</hidden>";
        assertEquals("<div class=\"hidden\"><input id=\"hiddenToggle7\" class=\"toggle\" type=\"checkbox\"><label for=\"hiddenToggle7\" class=\"hdn-toggle\" data-named=\"true\">Bark</label><div class=\"collapsible\">Something in  here</div></div>", doRender(namedHidden));
        String maliciousName = "<hidden name=\"<script>runsomething</script>\">Hidden</hidden>";
        RenderResult renderRes = underTest.renderWithInfo(maliciousName, "host", "site", "page", "user");
        assertEquals("<div class=\"hidden\"><input id=\"hiddenToggle11\" class=\"toggle\" type=\"checkbox\"><label for=\"hiddenToggle11\" class=\"hdn-toggle\" data-named=\"true\">&lt;script&gt;runsomething&lt;/script&gt;</label><div class=\"collapsible\">Hidden</div></div>", renderRes.renderedText());
        List<String> parseErrors = (List<String>) renderRes.renderState().get(RenderResult.RENDER_STATE_KEYS.ERRORS.name());
        assertEquals("Suspicious hidden tag name at 0. Raw text =[<script>runsomething</script>]", parseErrors.get(0));

        String unknownAttr = "<hidden fling=\"Bark\">Something in  here</hidden>";
        renderRes = underTest.renderWithInfo(unknownAttr, "host", "site", "page", "user");
        assertEquals("<div class=\"hidden\"><input id=\"hiddenToggle11\" class=\"toggle\" type=\"checkbox\"><label for=\"hiddenToggle11\" class=\"hdn-toggle\">Hidden</label><div class=\"collapsible\">Something in  here</div></div>", renderRes.renderedText());
        parseErrors = (List<String>) renderRes.renderState().get(RenderResult.RENDER_STATE_KEYS.ERRORS.name());
        assertEquals("Unknown attribute \"fling\" in hidden tag at 0", parseErrors.get(0));

        String hiddeonOnItsOwnLine= "<hidden>\n - Hidden with list\n -A list\n</hidden>";
        assertEquals("""
                <div class="hidden"><input id="hiddenToggle11" class="toggle" type="checkbox"><label for="hiddenToggle11" class="hdn-toggle">Hidden</label><div class="collapsible"><ol>
                <li>Hidden with list</li>
                <li>A list</li>
                </ol></div></div>""", doRender(hiddeonOnItsOwnLine));

        String hiddeonListStartsOnLine= "<hidden> - Hidden with list\n -A list\n</hidden>";
        assertEquals("""
                <div class="hidden"><input id="hiddenToggle11" class="toggle" type="checkbox"><label for="hiddenToggle11" class="hdn-toggle">Hidden</label><div class="collapsible"><ol>
                <li>Hidden with list</li>
                <li>A list</li>
                </ol></div></div>""", doRender(hiddeonListStartsOnLine));

        String nameWSpace = "<hidden name=\"Name w Space\">Text</hidden>";
        assertEquals("<div class=\"hidden\"><input id=\"hiddenToggle11\" class=\"toggle\" type=\"checkbox\"><label for=\"hiddenToggle11\" class=\"hdn-toggle\" data-named=\"true\">Name w Space</label><div class=\"collapsible\">Text</div></div>", doRender(nameWSpace));

        String twoParas = "<hidden>Para1\n\nPara2</hidden>";
        assertEquals("<div class=\"hidden\"><input id=\"hiddenToggle11\" class=\"toggle\" type=\"checkbox\"><label for=\"hiddenToggle11\" class=\"hdn-toggle\">Hidden</label><div class=\"collapsible\"><div>Para1</div>\n" +
                "<div>Para2</div></div></div>", doRender(twoParas));

        String codeBlockInHidden = "<hidden>  A Block</hidden>";
        assertEquals("<div class=\"hidden\"><input id=\"hiddenToggle11\" class=\"toggle\" type=\"checkbox\"><label for=\"hiddenToggle11\" class=\"hdn-toggle\">Hidden</label><div class=\"collapsible\"><pre class=\"code\">A Block\n" +
                "</pre></div></div>", doRender(codeBlockInHidden));

        String hiddenCloseInline = """
           <hidden> hidden closetag
           on sameline</hidden>""";
        assertEquals("""
                <div class="hidden"><input id="hiddenToggle11" class="toggle" type="checkbox"><label for="hiddenToggle11" class="hdn-toggle">Hidden</label><div class="collapsible">hidden closetag
                on sameline</div></div>""", doRender(hiddenCloseInline));
    }

    @Test
    public void testHidden_nested() {
        when(randomService.nextInt()).thenReturn(5,4);
        // Nested hidden doesn't work, the interior hidden is just escaped
        String nestedHidden = """
                <hidden> Hidden with
                <hidden>Nested hidden</hidden>
                </hidden>""";
        assertEquals("""
                <div class="hidden"><input id="hiddenToggle5" class="toggle" type="checkbox"><label for="hiddenToggle5" class="hdn-toggle">Hidden</label><div class="collapsible"><div> Hidden with</div>
                <div class="hidden"><input id="hiddenToggle4" class="toggle" type="checkbox"><label for="hiddenToggle4" class="hdn-toggle">Hidden</label><div class="collapsible">Nested hidden</div></div></div></div>""", doRender(nestedHidden));
    }

    @Test
    public void testHidden_unsupportedCases() {
        when(randomService.nextInt()).thenReturn(5);

        String notEnded = "<hidden>This hidden block is never closed";
        assertEquals("<div>&lt;hidden&gt;This hidden block is never closed</div>", doRender(notEnded));

    }

    @Test
    public void testRenderTOC() {
        String source = "====== Header 1 ======\n ==== Header 2 ====\n====== Header 3 ======\n===== Header 2 =====\n";
        String headerRender = """
                <div id="lw_TOC"></div>
                """;

        when(tocRenderService.renderTOC(any(), any())).thenReturn(headerRender);

        assertEquals(headerRender + "<h1 id=\"header_Header_1\">Header 1</h1>\n<h3 id=\"header_Header_2\">Header 2</h3>\n<h1 id=\"header_Header_3\">Header 3</h1>\n<h2 id=\"header_Header_2_1\">Header 2</h2>", doRender(source));
    }

    @Test
    public void testRenderNoTOC() {
        String source = "====== Header 1 ======\n ==== Header 2 ====\n====== Header 3 ======\n===== Header 2 =====\n  ~~NOTOC~~";
        String headerRender = """
                <div id="lw_TOC"></div>
                """;

        when(tocRenderService.renderTOC(any(), any())).thenReturn(headerRender);

        assertEquals("<h1 id=\"header_Header_1\">Header 1</h1>\n<h3 id=\"header_Header_2\">Header 2</h3>\n<h1 id=\"header_Header_3\">Header 3</h1>\n<h2 id=\"header_Header_2_1\">Header 2</h2>", doRender(source));
    }

    @Test
    public void testRenderYesTOC() {
        String source = "====== Header 1 ======\n ==== Header 2 ====\n  ~~YESTOC~~";
        String headerRender = """
                <div id="lw_TOC"></div>
                """;

        when(tocRenderService.renderTOC(any(), any())).thenReturn(headerRender);

        assertEquals(headerRender+"<h1 id=\"header_Header_1\">Header 1</h1>\n<h3 id=\"header_Header_2\">Header 2</h3>", doRender(source));
    }

    @Test
    public void testRenderHR() {
        String source="----";

        assertEquals("<hr>", doRender(source));
        source = "-----";
        assertEquals("<hr>", doRender(source));
    }

    @Test
    public void testRenderEmptyPage() {
        assertEquals("", doRender(""));
    }

    @Test
    public void testRenderUnknownTokens() {
        String source = "~~WHATTHIS";
        assertEquals("<div>~~WHATTHIS</div>", doRender(source));
    }
}