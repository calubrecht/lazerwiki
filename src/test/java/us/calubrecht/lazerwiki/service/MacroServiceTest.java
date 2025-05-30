package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.macro.CustomMacro;
import us.calubrecht.lazerwiki.macro.Macro;
import us.calubrecht.lazerwiki.model.PageCache;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.responses.PageData.PageFlags;
import us.calubrecht.lazerwiki.responses.SearchResult;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {MacroService.class, DokuWikiRenderer.class, RendererRegistrar.class, DokuWikiRendererTest.TestConfig.class},
        properties = { "lazerwiki.plugin.scan.packages=us.calubrecht.lazerwiki.service" })
@ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MacroServiceTest {

    @Autowired
    MacroService underTest;

    @MockBean
    PageService pageService;

    @MockBean
    MacroCssService macroCssService;

    @Autowired
    DokuWikiRenderer renderer;

    @MockBean
    LinkService linkService;

    @MockBean
    RandomService randomService;

    @MockBean
    LinkOverrideService linkOverrideService;

    @MockBean
    MediaOverrideService mediaOverrideService;

    @MockBean
    TOCRenderService tocRenderService;

    @Test
    @Order(1)
    void registerMacros() {
        // Macro was registerd and set CSS aat startup
        verify(macroCssService).addCss("div {}");
    }

    @Test
    @Order(2)
    void renderMacro() {
        RenderContext context = new RenderContext("localhost", "default", "page", "user");
        assertEquals("Good Macro", underTest.renderMacro("Good", "", context));

        // Unknown Macros give warning, macro name is sanitized
        assertEquals("MACRO- Unknown Macro &lt;div&gt;hi&lt;/div&gt;", underTest.renderMacro("<div>hi</div>:other text", "", context));

        // Recursive Macro does not continue to render itself
        context = new RenderContext("localhost", "default", "page", "user", renderer, new HashMap<>());
        assertEquals("Start:<div></div>", underTest.renderMacro("Recursive", "", context));
    }

    @Test
    @Order(3)
    void testMacroContextImpl() {
        RenderContext context = new RenderContext("localhost", "default", "page", "user");
        MacroService.MacroContextImpl macroContext = underTest.new MacroContextImpl(context);
        assertEquals("&lt;div&gt;hi&lt;/div&gt;", macroContext.sanitize("<div>hi</div>"));


        List<SearchResult> pages = List.of(
                new SearchResult("ns", "bigPage","Big Page",""),
                new SearchResult("", "otherPage","Other Page",""));
        when(pageService.searchPages(anyString(), anyString(), eq(Map.of("tag","aTag", "ns", "bigNS")))).thenReturn(Map.of("tag", pages));
        List<String> results = macroContext.getPagesByNSAndTag("bigNS", "aTag");
        assertEquals(2, results.size());
        assertEquals("ns:bigPage", results.get(0));
        assertEquals("otherPage", results.get(1));
    }

    @Test
    @Order(4)
    void testMacroContextImplRenderPage() {
        RenderContext context = new RenderContext("localhost", "default", "page", "user", renderer, new HashMap<>());
        MacroService.MacroContextImpl macroContext = underTest.new MacroContextImpl(context);
        PageData page = new PageData(null, "**Hi**", null, null, PageData.ALL_RIGHTS);
        when(pageService.getPageData(anyString(), eq("existsPage"), anyString())).thenReturn(page);
        assertEquals("<div><span class=\"bold\">Hi</span></div>", macroContext.renderPage("existsPage").getHtml());

        PageData notExists = new PageData("What is this?", null, null, null, new PageFlags(false, false, true, true, false, false));
        when(pageService.getPageData(anyString(), eq("notExists"), anyString())).thenReturn(notExists);
        assertEquals("", macroContext.renderPage("notExists").getHtml());

        PageData cantRead = new PageData("Not for you", null, null, null, new PageFlags(true, false, false, false, false, false));
        when(pageService.getPageData(anyString(), eq("cantRead"), anyString())).thenReturn(cantRead);
        assertEquals("", macroContext.renderPage("cantRead").getHtml());

        // Cahced Page
        PageCache cached = new PageCache();
        cached.useCache = true;
        cached.renderedCache = "This is from Cache";
        when(pageService.getCachedPage("localhost", "cachedPage")).thenReturn(cached);
        PageData renderedPage = new PageData(null, "Rendered now", null, null, PageData.ALL_RIGHTS);
        when(pageService.getPageData(anyString(), eq("cachedPage"), anyString())).thenReturn(renderedPage);
        assertEquals("This is from Cache", macroContext.renderPage("cachedPage").getHtml());

        PageCache ignoredCache = new PageCache();
        ignoredCache.useCache = false;
        ignoredCache.renderedCache = "This is from Cache";
        when(pageService.getCachedPage("localhost", "ignoredCache")).thenReturn(ignoredCache);
        when(pageService.getPageData(anyString(), eq("ignoredCache"), anyString())).thenReturn(renderedPage);
        assertEquals("<div>Rendered now</div>", macroContext.renderPage("ignoredCache").getHtml());
    }

    @Test
    @Order(5)
    void testRenderPageBroken() {
        RenderContext context = new RenderContext("localhost", "default", "page", "user");
        assertThrows(RuntimeException.class, ()-> underTest.renderMacro("Broken", "", context));
    }

    @Test
    @Order(6)
    void testMacroContextGetCachedRender() {
        //(boolean exists, boolean wasDeleted, boolean userCanRead, boolean userCanWrite, boolean userCanDelete) {
        PageData none = new PageData("", "", null, null, new PageData.PageFlags(false, false, true, true, true, false));
        PageData forbidden = new PageData("", "", null, null, new PageData.PageFlags(true, false, false, true, true, false));
        when(pageService.getPageData(any(), eq("noPage"), any())).thenReturn(none);
        when(pageService.getPageData(any(), eq("forbiddenPage"), any())).thenReturn(forbidden);
        RenderContext context = new RenderContext("localhost", "default", "page", "user", renderer, new HashMap<>());
        MacroService.MacroContextImpl macroContext = underTest.new MacroContextImpl(context);
        assertEquals("", macroContext.getCachedRender("noPage").getHtml());
        assertEquals("", macroContext.getCachedRender("forbiddenPage").getHtml());
        PageData ok = new PageData("", "OK page", null, null, new PageData.PageFlags(true, false, true, true, true, false));
        when(pageService.getPageData(any(), eq("cachedPage"), any())).thenReturn(ok);
        PageCache cached = new PageCache();
        cached.useCache = true;
        cached.renderedCache = "From cache";
        when(pageService.getCachedPage("localhost", "cachedPage")).thenReturn(cached);
        assertEquals("From cache", macroContext.getCachedRender("cachedPage").getHtml());
        when(pageService.getPageData(any(), eq("ignorableCache"), any())).thenReturn(ok);
        PageCache ignorableCache = new PageCache();
        ignorableCache.useCache = false;
        ignorableCache.renderedCache = "From cache but transient";
        when(pageService.getCachedPage("localhost", "ignorableCache")).thenReturn(ignorableCache);
        assertEquals("From cache but transient", macroContext.getCachedRender("ignorableCache").getHtml());
        when(pageService.getPageData(any(), eq("nonCached"), any())).thenReturn(ok);
        assertEquals("<div>OK page</div>", macroContext.getCachedRender("nonCached").getHtml());
    }

    @Test
    void testMacroContextGetCachedRenders() {
        PageData notExists = new PageData("", "", null, null, new PageData.PageFlags(false, false, true, true, true, false));
        PageData cannotRead = new PageData("", "", null, null, new PageData.PageFlags(true, false, false, true, true, false));
        PageData cached = new PageData("", "", null, null, new PageData.PageFlags(true, false, true, true, true, false));
        PageData notCached = new PageData("", "**notCachedPage**", null, null, new PageData.PageFlags(true, false, true, true, true, false));
        when(pageService.getPageData(any(), any(List.class), any())).thenReturn(
                Map.of(
                        new PageDescriptor("","cached"), cached,
                        new PageDescriptor("","notCached"), notCached,
                        new PageDescriptor("","notExists"), notExists,
                        new PageDescriptor("", "cannotRead"), cannotRead));
        when(pageService.getCachedPages(any(), any(List.class))).thenReturn(List.of(new PageCache("default", "", "cached", "cached Title", "rendered Cache", "plaintextCache", true)));

        RenderContext context = new RenderContext("localhost", "default", "page", "user", renderer, new HashMap<>());
        MacroService.MacroContextImpl macroContext = underTest.new MacroContextImpl(context);
        Map<String, Macro.MacroContext.RenderOutput> res = macroContext.getCachedRenders(List.of("cached", "notCached", "notExists", "cannotRead", "null"));

        assertEquals("rendered Cache", res.get("cached").getHtml());
        assertEquals("", res.get("notExists").getHtml());
        assertEquals("", res.get("cannotRead").getHtml());
        assertEquals("", res.get("null").getHtml());
        assertEquals("<div><span class=\"bold\">notCachedPage</span></div>", res.get("notCached").getHtml());
    }


    //setPageDontCache
    @Test
    @Order(7)
    void testSetPageDontCache() {
        RenderContext context = new RenderContext("localhost", "default", "page", "user", renderer, new HashMap<>());
        MacroService.MacroContextImpl macroContext = underTest.new MacroContextImpl(context);
        macroContext.setPageDontCache();
        assertEquals(true, context.renderState().get(RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name()));
    }

    @Test
    @Order(2)
    void renderMacroForCache() {
        RenderContext context = new RenderContext("localhost", "default", "page", "user");
        context.renderState().put(RenderResult.RENDER_STATE_KEYS.FOR_CACHE.name(), Boolean.TRUE);
        // Renders as expected because macro is not no-cache
        assertEquals("Good Macro", underTest.renderMacro("Good", "", context));
    }

    @Test
    void testPostRender() {
        RenderContext context = new RenderContext("localhost", "default", "page", "user");
        String text = "THis has some ~~MACRO~~Good:very/n/ngood~~/MACRO~~ macro in it";
        assertEquals("THis has some Good Macro macro in it", underTest.postRender(text, context));
    }

    @Test
    void testNoCacheMacro() {
        RenderContext context = new RenderContext("localhost", "default", "page", "user");
        context.renderState().put(RenderResult.RENDER_STATE_KEYS.FOR_CACHE.name(), Boolean.TRUE);
        String fullText = "~~MACRO~~NoCache~~/MACRO~~";
        String cached = underTest.renderMacro("NoCache", fullText, context);
        assertEquals(fullText, cached);
        context.renderState().remove(RenderResult.RENDER_STATE_KEYS.FOR_CACHE.name());
        assertEquals("Only render post cache", underTest.postRender(fullText, context));
    }


    @CustomMacro
    public static class BrokenMacro extends Macro {
        @Override
        public String getName() {
            return "Broken";
        }

        public Optional<String> getCSS() {
            throw new RuntimeException("BOOM!");
        }

        @Override
        public String render(MacroContext context, String macroArgs) {
            throw new RuntimeException("More boom!");
        }
    }

    @CustomMacro
    public static class GoodMacro extends Macro {
        @Override
        public String getName() {
            return "Good";
        }

        public Optional<String> getCSS() {
            return Optional.of("div {}");
        }

        @Override
        public String render(MacroContext context, String macroArgs) {
            return "Good Macro";
        }
    }

    @CustomMacro
    public static class RecursiveMacro extends Macro {
        @Override
        public String getName() {
            return "Recursive";
        }

        @Override
        public String render(MacroContext context, String macroArgs) {
            return "Start:" + context.renderMarkup("~~MACRO~~Recursive~~/MACRO~~").getHtml();
        }
    }

    @CustomMacro
    public static class NoCacheMAcro extends Macro {
        @Override
        public String getName() {
            return "NoCache";
        }

        @Override
        public boolean allowCache(MacroContext context, String macroArgs) {
            return false;
        }

        @Override
        public String render(MacroContext context, String macroArgs) {
            return "Only render post cache";
        }
    }

}