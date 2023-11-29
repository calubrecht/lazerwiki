package us.calubrecht.lazerwiki.exampleMacros;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.service.*;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

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


    @Test
    void render() {
        RenderContext renderContext = new RenderContext("localhost", "default", "user", renderer, new HashMap<>());
        assertEquals("<div class=\"special\">This is some special text</div>", macroService.renderMacro("wrap:special:This is some special text", renderContext));
    }
}