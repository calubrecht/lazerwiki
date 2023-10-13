package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = { DokuWikiRenderer.class })
@ActiveProfiles("test")
class DokuWikiRendererTest {

    @Autowired
    DokuWikiRenderer underTest;

    @Test
    void testRenderHeader() {
        String source = "====== Big header ======\n==== Smaller Header ====";

        assertEquals("<h1>Big header</h1>\n<h3>Smaller Header</h3>", underTest.render(source));

        assertEquals("<h2>Header with space.</h2>", underTest.render("=====Header with space.===== "));

        assertEquals("===Doesn't parse as header=== with trailing", underTest.render("===Doesn't parse as header=== with trailing"));
    }
}