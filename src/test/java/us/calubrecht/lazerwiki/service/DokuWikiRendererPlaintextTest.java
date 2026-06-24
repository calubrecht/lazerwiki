package us.calubrecht.lazerwiki.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

@SpringBootTest(classes = {CustomWikiRenderer.class, DokuWikiRendererTest.TestConfig.class})
@ActiveProfiles("test")
public class DokuWikiRendererPlaintextTest {
  @MockitoBean TOCRenderService tocRenderService;

  @Configuration
  @ComponentScan({"us.calubrecht.lazerwiki.syntax"})
  public static class TestConfig {}

  @Autowired CustomWikiRenderer underTest;

  @MockitoBean PageService pageService;

  @MockitoBean MacroService macroService;

  @MockitoBean RandomService randomService;

  @MockitoBean LinkOverrideService linkOverrideService;

  @MockitoBean MediaOverrideService mediaOverrideService;

  String doRender(String source) {
    RenderContext context = new RenderContext("localhost", "default", "page", "jack");
    return underTest.renderWithInfo(source, context).plainText();
  }

  @Test
  void test_renderLink() {

    when(pageService.exists(eq("localhost"), eq("exists"))).thenReturn(true);
    when(pageService.getTitle(eq("localhost"), eq("exists"))).thenReturn("This Page Exists");

    assertEquals("This Page Exists", doRender("[[exists]]"));
    assertEquals(
        "This Page has an alternate display",
        doRender("[[ exists|This Page has an alternate display]]"));
    assertEquals("httP://externalLink.com", doRender("[[httP://externalLink.com ]]"));

    assertEquals("This Page Exists", doRender("[[exists| ]]"));
  }

  @Test
  void test_renderList() {
    assertEquals("Item 1\nItem2", doRender("  *Item 1\n  *Item2"));
  }

  @Test
  void test_renderHeader() {
    assertEquals("The Header", doRender("===The Header==="));

    assertEquals("The Header with a link", doRender("===The Header with a [[goHere|link]]==="));
  }

  @Test
  void test_renderImg() {
    assertEquals("thisImage.jpg", doRender("{{thisImage.jpg}}"));

    assertEquals("Image with a title", doRender("{{thisImage.jpg|Image with a title}}"));
  }

  @Test
  void test_renderCodeBox() {
    assertEquals("This is in the code block", doRender("  This is in the code block"));
  }

  @Test
  void test_renderMacro() {
    when(macroService.renderMacro(anyString(), anyString(), any()))
        .thenReturn("Something that won't show in plaintext");
    assertEquals("", doRender("~~MACRO~~This could be any macro~~/MACRO~~"));
  }

  @Test
  void test_renderSpans() {
    assertEquals(
        "Bold, italic, none of it is rendered",
        doRender("**Bold, //italic//**, none of it is rendered"));
  }

  @Test
  public void test_renderUnformat() {
    String input1 = "%%This **should not be bold**%%";
    assertEquals("This **should not be bold**", doRender(input1));
  }

  @Test
  public void test_renderLinebreak() {
    String input = "This is a line \\\\ with a linebreak";
    assertEquals("This is a line\n with a linebreak", doRender(input));
  }

  @Test
  public void test_renderTable() {
    String inputSimpleTable = "| First | Line |\n|Second | Line|";
    assertEquals("| First | Line |\n" + "|Second | Line|", doRender(inputSimpleTable));
  }

  @Test
  public void test_renderBlockquote() {
    String inputBlockquote = "> One quote **with some bold**\n>And\n>>Another layer of quote";
    assertEquals(
        " One quote with some bold\nAnd\nAnother layer of quote", doRender(inputBlockquote));
  }

  @Test
  public void test_hidden() {
    when(randomService.nextInt()).thenReturn(5, 8);
    String inputBlockquote = "<hidden>simple</hidden>";
    assertEquals("simple", doRender(inputBlockquote));

    assertEquals(
        "line1\n\nline2with some title text",
        doRender("<hidden>line1\n\nline2{{animage|with some title text}}</hidden>"));

    String namedHidden = "<hidden name=\"Bark\">Something in  here</hidden>";
    assertEquals("Bark:Something in  here", doRender(namedHidden));
  }

  @Test
  public void test_renderNoTOC() {
    String source =
        "====== Header 1 ======\n ==== Header 2 ====\n====== Header 3 ======\n===== Header 2 =====\n  ~~NOTOC~~";
    String headerRender =
        """
                <div id="lw_TOC"></div>
                """;

    when(tocRenderService.renderTOC(any(), any())).thenReturn(headerRender);

    assertEquals(" Header 1 \n Header 2 \n Header 3 \n Header 2", doRender(source));
  }

  @Test
  public void test_renderBrokenInput() {
    String source = "---";
    // If can't figure it out, just print it raw
    assertEquals("---", doRender(source));
  }

  @Test
  public void test_renderHR() {
    String source = "----";

    assertEquals("----", doRender(source));
    source = "-----";
    assertEquals("-----", doRender(source));
  }

  @Test
  public void test_htmlEntities() {
    String source = "<Ä   #";

    // Plaintext renderer should not replace with entities
    assertEquals(source, doRender(source));
  }
}
