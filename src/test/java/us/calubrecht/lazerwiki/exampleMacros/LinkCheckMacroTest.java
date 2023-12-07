package us.calubrecht.lazerwiki.exampleMacros;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.repository.PageCacheRepository;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.service.*;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {MacroService.class, DokuWikiRenderer.class, RendererRegistrar.class, us.calubrecht.lazerwiki.service.DokuWikiRendererTest.TestConfig.class})
@ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
@ActiveProfiles("test")
public class LinkCheckMacroTest {
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

    @Test
    public void testChecklinks() {
        RenderContext renderContext = new RenderContext("localhost", "default", "user", renderer, new HashMap<>());
        when(pageService.getAllPagesFlat("localhost", "user")).thenReturn(List.of("", "page2", "page3"));
        when(linkService.getLinksOnPage("default","")).thenReturn(List.of("page2", "page5"));
        when(linkService.getLinksOnPage("default","page2")).thenReturn(List.of("page8"));
        when(linkService.getLinksOnPage("default","page3")).thenReturn(List.of("page2"));

        String rendered = macroService.renderMacro("linkCheck", renderContext);
        String[] split = rendered.split("Orphaned Pages");

        // Broken links section. Split on 2 tds will give 4 parts, so 2 broken links(page5,page8) + 1 header
        assertEquals(4, split[0].split("<tr>").length);
        assertTrue(split[0].indexOf("HOME") != -1);

        //Orphan pages section. Split on 1 td will give 3 parts so 1 orphaned pages(page3) + 1 header
        assertEquals(3, split[1].split("<tr>").length);

        assertTrue((Boolean)renderContext.renderState().get(RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name()));
    }
}
