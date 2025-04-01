package us.calubrecht.lazerwiki.exampleMacros;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.service.*;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.LINKS;

@SpringBootTest(classes = {MacroService.class, DokuWikiRenderer.class, RendererRegistrar.class, us.calubrecht.lazerwiki.service.DokuWikiRendererTest.TestConfig.class})
@ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
@ActiveProfiles("test")
class WrapMacroTest {

    @Autowired
    MacroService macroService;

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

    RenderContext context() {
        return new RenderContext("localhost", "default", "page", "user", renderer, new HashMap<>());
    }

    @Test
    void render() {
        RenderContext renderContext = context();
        assertEquals("<div class=\"special\">This is some special text</div>", macroService.renderMacro("wrap:special:This is some special text", "", renderContext));
    }

    @Test
    void renderMultiline() {
        RenderContext renderContext = context();
        assertEquals("<div class=\"special\"><div>This is some special text</div>\n<div>OnMultiple lines</div></div>", macroService.renderMacro("wrap:special:This is some special text\n\nOnMultiple lines", "", renderContext));
    }

    @Test
    void renderSimple() {
        RenderContext renderContext = context();
        assertEquals("<div class=\"justTag\"></div>", macroService.renderMacro("wrap:justTag", "", renderContext));
        assertEquals("<div class=\"justTag\"></div>", macroService.renderMacro("wrap:justTag:", "", renderContext));
    }

    @Test
    void renderWithLinks() {
        when(pageService.getTitle(anyString(), anyString())).thenReturn("title");
        RenderContext renderContext = context();
        macroService.renderMacro("wrap:withLink:[[aLink]]", "", renderContext);

        assertEquals(new HashSet<>(Arrays.asList("aLink")), renderContext.renderState().get(LINKS.name()));

        renderContext = context();
        renderContext.renderState().put(LINKS.name(), new HashSet<>(Arrays.asList("existingLink")));
        macroService.renderMacro("wrap:withLink:[[aLink]]", "", renderContext);

        assertEquals(new HashSet<>(Arrays.asList("aLink", "existingLink")), renderContext.renderState().get(LINKS.name()));
    }
}