package us.calubrecht.lazerwiki.exampleMacros;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.LinkOverride;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.repository.PageCacheRepository;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.service.*;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
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

    @MockBean
    RandomService randomService;

    @MockBean
    LinkOverrideService linkOverrideService;

    @MockBean
    MediaOverrideService mediaOverrideService;

    @MockBean
    TOCRenderService tocRenderService;

    @Test
    public void testChecklinks() {
        RenderContext renderContext = new RenderContext("localhost", "default", "page", "user", renderer, new HashMap<>());
        when(pageService.getAllPagesFlat("localhost", "user")).thenReturn(List.of("", "page2", "page3"));
        when(pageService.isReadable(eq("localhost"), anyString(), anyString())).thenReturn(true);
        when(linkService.getLinksOnPage("default","")).thenReturn(List.of("page2", "page5"));
        when(linkService.getLinksOnPage("default","page2")).thenReturn(List.of("ns:page8"));
        when(linkService.getLinksOnPage("default","page3")).thenReturn(List.of("page2"));

        String rendered = macroService.renderMacro("linkCheck", "", renderContext);
        String[] split = rendered.split("Orphaned Pages");

        // Broken links section. Split on 3trs will give 4 parts, so 2 broken links(page5,page8) + 1 header
        assertEquals(4, split[0].split("<tr>").length);
        assertTrue(split[0].indexOf("HOME") != -1);

        //Orphan pages section. Split on 2 trs will give 3 parts so 1 orphaned pages(page3) + 1 header
        assertEquals(3, split[1].split("<tr>").length);

        assertTrue((Boolean)renderContext.renderState().get(RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name()));
    }

    @Test
    public void testgetNS() {
        LinkCheckMacro linkCheckMacro = new LinkCheckMacro();
        assertEquals("", linkCheckMacro.getNS(""));
        assertEquals("", linkCheckMacro.getNS("noNS"));
        assertEquals("ns:", linkCheckMacro.getNS("ns:oneNS"));
        assertEquals("ns:ns2:", linkCheckMacro.getNS("ns:ns2:twoNS"));
    }

    @Test
    public void testChecklinks_Filering() {
        RenderContext renderContext = new RenderContext("localhost", "default", "page", "user", renderer, new HashMap<>());
        when(pageService.getAllPagesFlat("localhost", "user")).thenReturn(List.of("_meta:metaPage", "ns1:nsPage", "ns2:ns2Page", "anyNS:_template", "_template","notOrphan:notorpahn","noPage10"));
        when(linkService.getLinksOnPage("default","_meta:metaPage")).thenReturn(List.of("noPage1"));
        when(linkService.getLinksOnPage("default","anyNS:_template")).thenReturn(List.of("noPage4"));
        when(linkService.getLinksOnPage("default","_template")).thenReturn(List.of("noPage5"));
        when(linkService.getLinksOnPage("default","ns1:nsPage")).thenReturn(List.of("noPage2","noPage3"));
        when(linkService.getLinksOnPage("default","ns2:ns2Page")).thenReturn(List.of("noPage3"));
        when(linkService.getLinksOnPage("default", "notOrphan:notorpahn")).thenReturn(List.of("noPage10", "missingPage"));
        when(pageService.isReadable(eq("localhost"), anyString(), anyString())).thenReturn(true);

        String rendered = macroService.renderMacro("linkCheck", "", renderContext);
        String[] split = rendered.split("Orphaned Pages");
        // Broken links section. Split on 4 trs will give 5 parts, so 3 broken link 1 header (_meta page is ignored)
        assertEquals(5, split[0].split("<tr>").length);
        // Orphans links section. Split on 4 trs will give 5 parts, so 3 orphans li1 header (_meta page is ignored)
        assertEquals(5, split[1].split("<tr>").length);

        rendered = macroService.renderMacro("linkCheck:filterNS=ns1", "", renderContext);
        split = rendered.split("Orphaned Pages");
        // 1 Broken link (_meta and ns1 are ignored);
        assertEquals(4, split[0].split("<tr>").length);

        rendered = macroService.renderMacro("linkCheck:ns=ns2",  "", renderContext);
        split = rendered.split("Orphaned Pages");
        // 1 Broken link (only ns2 is looked at)
        assertEquals(3, split[0].split("<tr>").length);

        // allow trailing :
        rendered = macroService.renderMacro("linkCheck:filterNS=ns1:", "", renderContext);
        split = rendered.split("Orphaned Pages");
        // 1 Broken link (_meta and ns1 are ignored);
        assertEquals(4, split[0].split("<tr>").length);

        rendered = macroService.renderMacro("linkCheck:ns=ns2:", "", renderContext);
        split = rendered.split("Orphaned Pages");
        // 1 Broken link (only ns2 is looked at)
        assertEquals(3, split[0].split("<tr>").length);

        rendered = macroService.renderMacro("linkCheck:filterOrphanNS=notOrphan", "", renderContext);
        split = rendered.split("Orphaned Pages");
        // Counts broken link on notOrphan page
        assertEquals(5, split[0].split("<tr>").length);
        // doesn't count notOrphan as orphan
        assertEquals(4, split[1].split("<tr>").length);

        rendered = macroService.renderMacro("linkCheck:filterOrphanNS=notOrphan:", "", renderContext);
        split = rendered.split("Orphaned Pages");
        // Counts broken link on notOrphan page
        assertEquals(5, split[0].split("<tr>").length);
        // doesn't count notOrphan as orphan
        assertEquals(4, split[1].split("<tr>").length);

    }

    @Test
        public void testLinkCheckMacroForCache() {
        RenderContext renderContext = new RenderContext("localhost", "default", "page", "user", renderer, new HashMap<>());
        renderContext.renderState().put(RenderResult.RENDER_STATE_KEYS.FOR_CACHE.name(), Boolean.TRUE);
        PageData page = new PageData(null, "This Page", null, null, PageData.ALL_RIGHTS);
        when(pageService.getPageData(anyString(), eq("includedPage"), anyString())).thenReturn(page);
        assertEquals("~~MACRO~~linkCheck:filterOrphanNS=notOrphan:~~/MACRO~~", macroService.renderMacro("linkCheck:filterOrphanNS=notOrphan:", "~~MACRO~~linkCheck:filterOrphanNS=notOrphan:~~/MACRO~~", renderContext));
        // Did not render macro, safe to cache.
        assertNull((Boolean)renderContext.renderState().get(RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name()));
    }

    @Test
    public void testChecklinksForReadable() {
        RenderContext renderContext = new RenderContext("localhost", "default", "page", "user", renderer, new HashMap<>());
        when(pageService.getAllPagesFlat("localhost", "user")).thenReturn(List.of("", "page2", "page3"));
        when(pageService.isReadable(eq("localhost"), eq("page5"), anyString())).thenReturn(true);
        when(linkService.getLinksOnPage("default","")).thenReturn(List.of("page2", "page5"));
        when(linkService.getLinksOnPage("default","page2")).thenReturn(List.of("ns:page8"));
        when(linkService.getLinksOnPage("default","page3")).thenReturn(List.of("page2"));

        String rendered = macroService.renderMacro("linkCheck", "", renderContext);
        String[] split = rendered.split("Orphaned Pages");

        // Broken links section. Split gives 3 parts = 2 rows, so 1 broken links(page5) + 1 header
        assertEquals(3, split[0].split("<tr>").length);
        assertTrue(split[0].indexOf("HOME") != -1);
    }

    @Test
    public void testChecklinksWoverrides() {
        RenderContext renderContext = new RenderContext("localhost", "default", "page", "user", renderer, new HashMap<>());
        when(pageService.getAllPagesFlat("localhost", "user")).thenReturn(List.of("", "ns1:movePage2", "page3"));
        when(pageService.isReadable(eq("localhost"), anyString(), anyString())).thenReturn(true);
        when(linkService.getLinksOnPage("default","")).thenReturn(List.of("page2", "page5"));
        when(linkOverrideService.getOverrides("localhost", "")).thenReturn(List.of(
                new LinkOverride("default", "", "", "", "page2", "ns1", "MovePage2")
                ));

        String rendered = macroService.renderMacro("linkCheck", "", renderContext);
        String[] split = rendered.split("Orphaned Pages");

        //Orphan pages section. Split on 2 trs will give 3 parts so 1 orphaned pages(page3) + 1 header
        assertEquals(3, split[1].split("<tr>").length);
        assertTrue(split[1].split("<tr>")[2].contains("page3"));

        assertTrue((Boolean)renderContext.renderState().get(RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name()));
    }
}
