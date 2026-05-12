package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("unchecked")
@SpringBootTest(classes = { CustomWikiRenderer.class})
@ComponentScan("us.calubrecht.lazerwiki.service.renderhelpers.doku")
@ActiveProfiles("test")
public class DokuWikiRender2Test {
    @Autowired
    CustomWikiRenderer underTest;

    @MockBean
    PageService pageService;

    @MockBean
    MacroService macroService;

    @MockBean
    RandomService randomService;

    @MockBean
    LinkOverrideService linkOverrideService;

    @MockBean
    MediaOverrideService mediaOverrideService;

    String doRender(String source) {
        return underTest.renderToString(source, "localhost", "default", "page", "");
    }

    @Test
    void testRenderHeader() {
        String source = "====== Big header ======\n ==== Smaller Header ====";

        assertEquals("<h1 id=\"header_Big_header\">Big header</h1>\n<h3 id=\"header_Smaller_Header\">Smaller Header</h3>", doRender(source));

        assertEquals("<h2 id=\"header_Header_with_space.\">Header with space.</h2>", doRender("=====Header with space.===== "));

        assertEquals("<div>===Doesn't parse as header=== with trailing</div>", doRender("===Doesn't parse as header=== with trailing"));
    }
}
