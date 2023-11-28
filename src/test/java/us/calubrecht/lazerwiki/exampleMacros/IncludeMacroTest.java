package us.calubrecht.lazerwiki.exampleMacros;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.service.*;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {MacroService.class, DokuWikiRenderer.class, RendererRegistrar.class, us.calubrecht.lazerwiki.service.DokuWikiRendererTest.TestConfig.class})
@ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
@ActiveProfiles("test")
class IncludeMacroTest {

    @Autowired
    MacroService macroService;

    @MockBean
    PageService pageService;

    @MockBean
    MacroCssService macroCssService;

    @Autowired
    DokuWikiRenderer renderer;

    @Test
    public void testIncludeMacro() {
        RenderContext renderContext = new RenderContext("localhost", "default", "user", renderer, new HashMap<>());
        PageData page = new PageData(null, "This Page", null, true, true, true);
        when(pageService.getPageData(anyString(), eq("includedPage"), anyString())).thenReturn(page);
        assertEquals("<div>This Page</div>", macroService.renderMacro("include:includedPage", renderContext));

        PageData notpage = new PageData(null, "", null, false, true, true);
        when(pageService.getPageData(anyString(), eq("nothingPage"), anyString())).thenReturn(notpage);
        assertEquals("", macroService.renderMacro("include:nothingPage", renderContext));
    }

}