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
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    @Order(1)
    void registerMacros() {
        // Macro was registerd and set CSS aat startup
        verify(macroCssService).addCss("div {}");
    }

    @Test
    @Order(2)
    void renderMacro() {
        RenderContext context = new RenderContext("localhost", "default", "user");
        assertEquals("Good Macro", underTest.renderMacro("Good", context));

        // Unknown Macros give warning, macro name is sanitized
        assertEquals("MACRO- Unknown Macro &lt;div&gt;hi&lt;/div&gt;", underTest.renderMacro("<div>hi</div>:other text", context));

        // Recursive Macro does not continue to render itself
        context = new RenderContext("localhost", "default", "user", renderer, new HashMap<>());
        assertEquals("Start:<div></div>", underTest.renderMacro("Recursive", context));
    }

    @Test
    @Order(3)
    void testMacroContextImpl() {
        RenderContext context = new RenderContext("localhost", "default", "user");
        MacroService.MacroContextImpl macroContext = underTest.new MacroContextImpl(context);
        assertEquals("&lt;div&gt;hi&lt;/div&gt;", macroContext.sanitize("<div>hi</div>"));


        List<PageDesc> pages = List.of(new PageServiceTest.PageDescImpl("ns", "bigPage","Big Page",""));
        when(pageService.searchPages(anyString(), anyString(), eq(Map.of("tag","aTag", "ns", "bigNS")))).thenReturn(pages);
        List<String> results = macroContext.getPagesByNSAndTag("bigNS", "aTag");
        assertEquals(1, results.size());
        assertEquals("ns:bigPage", results.get(0));
    }

    @Test
    @Order(4)
    void testMacroContextImplRenderPage() {
        RenderContext context = new RenderContext("localhost", "default", "user", renderer, new HashMap<>());
        MacroService.MacroContextImpl macroContext = underTest.new MacroContextImpl(context);
        PageData page = new PageData(null, "**Hi**", null, true, true, true);
        when(pageService.getPageData(anyString(), eq("existsPage"), anyString())).thenReturn(page);
        assertEquals("<div><span class=\"bold\">Hi</span></div>", macroContext.renderPage("existsPage").getLeft());

        PageData notExists = new PageData("What is this?", null, null, false, true, true);
        when(pageService.getPageData(anyString(), eq("notExists"), anyString())).thenReturn(notExists);
        assertEquals("", macroContext.renderPage("notExists").getLeft());

        PageData cantRead = new PageData("Not for you", null, null, true, false, false);
        when(pageService.getPageData(anyString(), eq("cantRead"), anyString())).thenReturn(cantRead);
        assertEquals("", macroContext.renderPage("cantRead").getLeft());

    }

    @Test
    @Order(5)
    void testRenderPageBroken() {
        RenderContext context = new RenderContext("localhost", "default", "user");
        assertThrows(RuntimeException.class, ()-> underTest.renderMacro("Broken", context));

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
            return "Start:" + context.renderMarkup("~~MACRO~~Recursive~~/MACRO~~").getLeft();
        }
    }

}