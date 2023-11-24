package us.calubrecht.lazerwiki.service;

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

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = { DokuWikiRenderer.class, RendererRegistrar.class, ProblemPagesTest.TestConfig.class})
@ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
@ActiveProfiles("test")
public class ProblemPagesTest {

    @Autowired
    DokuWikiRenderer underTest;

    @MockBean
    PageService pageService;

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
        String rendered = underTest.render(s, "default");

        assertTrue(rendered != null);

    }
}
