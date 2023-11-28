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
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {MacroService.class, MacroServiceTest.BrokenMacro.class, MacroServiceTest.GoodMacro.class, DokuWikiRenderer.class, RendererRegistrar.class, DokuWikiRendererTest.TestConfig.class})
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
            return "";
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