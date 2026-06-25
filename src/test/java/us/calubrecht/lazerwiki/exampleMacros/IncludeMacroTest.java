package us.calubrecht.lazerwiki.exampleMacros;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.responses.PageData.PageFlags;
import us.calubrecht.lazerwiki.service.*;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

@SpringBootTest(
    classes = {
      MacroService.class,
      CustomWikiRenderer.class,
      us.calubrecht.lazerwiki.service.DokuWikiRendererTest.TestConfig.class
    })
@ActiveProfiles("test")
class IncludeMacroTest {

  @Autowired MacroService macroService;

  @MockitoBean PageService pageService;

  @MockitoBean PageSearchService pageSearchService;

  @MockitoBean MacroCssService macroCssService;

  @Autowired CustomWikiRenderer renderer;

  @MockitoBean LinkService linkService;

  @MockitoBean RandomService randomService;

  @MockitoBean LinkOverrideService linkOverrideService;

  @MockitoBean MediaOverrideService mediaOverrideService;

  @MockitoBean TOCRenderService tocRenderService;

  @Test
  public void test_includeMacro() {
    RenderContext renderContext =
        new RenderContext("default", "page", "user", renderer, new HashMap<>());
    PageData page = new PageData(null, "This Page", null, null, PageData.ALL_RIGHTS);
    when(pageService.getPageData(anyString(), eq("includedPage"), anyString())).thenReturn(page);
    assertEquals(
        "<div class=\"include\"><div>This Page</div><a href=\"/page/includedPage#Edit\" className=\"includePageLink\">Edit includedPage</a></div>",
        macroService.renderMacro("include:includedPage", "", renderContext));
    assertTrue(
        (Boolean) renderContext.renderState().get(RenderResult.RenderStateKeys.DONT_CACHE.name()));
    // without write  rights.
    PageData roPage =
        new PageData(
            null, "RO Page", null, null, new PageFlags(true, false, true, false, false, false));
    when(pageService.getPageData(anyString(), eq("roPage"), anyString())).thenReturn(roPage);
    assertEquals(
        "<div class=\"include\"><div>RO Page</div></div>",
        macroService.renderMacro("include:roPage", "", renderContext));
    assertTrue(
        (Boolean) renderContext.renderState().get(RenderResult.RenderStateKeys.DONT_CACHE.name()));

    PageData notpage =
        new PageData(null, "", null, null, new PageFlags(false, false, true, false, false, false));
    when(pageService.getPageData(anyString(), eq("nothingPage"), anyString())).thenReturn(notpage);
    assertEquals(
        "<div class=\"include\"></div>",
        macroService.renderMacro("include:nothingPage", "", renderContext));
    assertTrue(
        (Boolean) renderContext.renderState().get(RenderResult.RenderStateKeys.DONT_CACHE.name()));

    RenderContext plaintextContext =
        new RenderContext(
            "default",
            "page",
            "user",
            renderer,
            new HashMap<>(Map.of("plainText", true)));
    assertEquals("", macroService.renderMacro("include:includedPage", "", plaintextContext));
  }

  @Test
  public void test_includeMacroForCache() {
    RenderContext renderContext =
        new RenderContext("default", "page", "user", renderer, new HashMap<>());
    renderContext.renderState().put(RenderResult.RenderStateKeys.FOR_CACHE.name(), Boolean.TRUE);
    PageData page = new PageData(null, "This Page", null, null, PageData.ALL_RIGHTS);
    when(pageService.getPageData(anyString(), eq("includedPage"), anyString())).thenReturn(page);
    assertEquals(
        "~~MACRO~~include:1~~/MACRO~~",
        macroService.renderMacro(
            "include:includedPage", "~~MACRO~~include:1~~/MACRO~~", renderContext));
    // Did not render macro, safe to cache.
    assertNull(
        (Boolean) renderContext.renderState().get(RenderResult.RenderStateKeys.DONT_CACHE.name()));
  }
}
