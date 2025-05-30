package us.calubrecht.lazerwiki.service;

import org.antlr.v4.runtime.TokenStreamRewriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = { DokuWikiRenderer.class, RendererRegistrar.class, ProblemPagesTest.TestConfig.class, MacroService.class})
@ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
@ActiveProfiles("test")
public class ProblemPagesTest {

    @Autowired
    DokuWikiRenderer underTest;

    @MockBean
    PageService pageService;

    @MockBean
    MacroCssService macroCssService;

    @MockBean
    LinkService linkService;

    @MockBean
    RandomService randomService;

    @MockBean
    LinkOverrideService linkOverrideService;

    @MockBean
    MediaOverrideService mediaOverrideService;

    @MockBean
    TOCRenderService rocRenderService;

    @Configuration
    @ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
    public static class TestConfig {
    }

    String loadPage(String pageName) {
        File f = Paths.get("src/test/resources/problem_pages", pageName).toFile();
        try (FileInputStream fis = new FileInputStream(f)){
            byte[] bytesRead = fis.readAllBytes();
            fis.close();
            return new String(bytesRead);
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    @Test
    public void testAboutPage_wasFatal() {
        String s = loadPage("about.page") + '\n';
        String rendered = underTest.renderToString(s,  "localhost","default",  "page","");

        assertTrue(rendered != null);

    }

    @Test
    public void testWrapMacro_wasFatal() {
        String s = loadPage("wrapMacro.page") + '\n';
        String rendered = underTest.renderToString(s,  "localhost","default", "page", "");

        assertTrue(rendered != null);

    }
}
