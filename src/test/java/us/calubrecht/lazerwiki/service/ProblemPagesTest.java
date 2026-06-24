package us.calubrecht.lazerwiki.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    classes = {CustomWikiRenderer.class, ProblemPagesTest.TestConfig.class, MacroService.class})
@ActiveProfiles("test")
public class ProblemPagesTest {

  @Autowired CustomWikiRenderer underTest;

  @MockitoBean PageService pageService;

  @MockitoBean PageSearchService pageSearchService;

  @MockitoBean MacroCssService macroCssService;

  @MockitoBean LinkService linkService;

  @MockitoBean RandomService randomService;

  @MockitoBean LinkOverrideService linkOverrideService;

  @MockitoBean MediaOverrideService mediaOverrideService;

  @MockitoBean TOCRenderService rocRenderService;

  @Configuration
  @ComponentScan("us.calubrecht.lazerwiki.syntax")
  public static class TestConfig {}

  String loadPage(String pageName) {
    File f = Paths.get("src/test/resources/problem_pages", pageName).toFile();
    try (FileInputStream fis = new FileInputStream(f)) {
      byte[] bytesRead = fis.readAllBytes();
      fis.close();
      return new String(bytesRead);
    } catch (IOException e) {
      return null;
    }
  }

  @Test
  public void test_aboutPage_wasFatal() {
    String s = loadPage("about.page") + '\n';
    String rendered = underTest.renderToString(s, "localhost", "default", "page", "");

    assertTrue(rendered != null);
  }

  @Test
  public void test_wrapMacro_wasFatal() {
    String s = loadPage("wrapMacro.page") + '\n';
    String rendered = underTest.renderToString(s, "localhost", "default", "page", "");

    assertTrue(rendered != null);
  }
}
