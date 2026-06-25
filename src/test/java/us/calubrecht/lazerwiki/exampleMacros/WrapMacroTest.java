package us.calubrecht.lazerwiki.exampleMacros;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static us.calubrecht.lazerwiki.model.RenderResult.RenderStateKeys.LINKS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import us.calubrecht.lazerwiki.service.*;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

@SpringBootTest(
    classes = {
      MacroService.class,
      CustomWikiRenderer.class,
      us.calubrecht.lazerwiki.service.DokuWikiRendererTest.TestConfig.class
    })
@ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
@ActiveProfiles("test")
class WrapMacroTest {

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

  RenderContext context() {
    return new RenderContext("default", "page", "user", renderer, new HashMap<>());
  }

  @Test
  void test_render() {
    RenderContext renderContext = context();
    assertEquals(
        "<div class=\"special\">This is some special text</div>",
        macroService.renderMacro("wrap:special:This is some special text", "", renderContext));
  }

  @Test
  void test_renderMultiline() {
    RenderContext renderContext = context();
    assertEquals(
        "<div class=\"special\"><div>This is some special text</div>\n<div>OnMultiple lines</div></div>",
        macroService.renderMacro(
            "wrap:special:This is some special text\n\nOnMultiple lines", "", renderContext));
  }

  @Test
  void test_renderSimple() {
    RenderContext renderContext = context();
    assertEquals(
        "<div class=\"justTag\"></div>",
        macroService.renderMacro("wrap:justTag", "", renderContext));
    assertEquals(
        "<div class=\"justTag\"></div>",
        macroService.renderMacro("wrap:justTag:", "", renderContext));
  }

  @Test
  void test_renderWithLinks() {
    when(pageService.getTitle(anyString(), anyString())).thenReturn("title");
    RenderContext renderContext = context();
    macroService.renderMacro("wrap:withLink:[[aLink]]", "", renderContext);

    assertEquals(
        new HashSet<>(Arrays.asList("aLink")), renderContext.renderState().get(LINKS.name()));

    renderContext = context();
    renderContext.renderState().put(LINKS.name(), new HashSet<>(Arrays.asList("existingLink")));
    macroService.renderMacro("wrap:withLink:[[aLink]]", "", renderContext);

    assertEquals(
        new HashSet<>(Arrays.asList("aLink", "existingLink")),
        renderContext.renderState().get(LINKS.name()));
  }
}
