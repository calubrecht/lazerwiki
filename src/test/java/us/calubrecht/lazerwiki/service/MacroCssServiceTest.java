package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MacroCssServiceTest {

    MacroCssService service= new MacroCssService();

    @Test
    void getCss() {
        service.addCss("div.bold { font-weight: \"bold\"}");

        assertNull(service.constructedCss);
        assertEquals("div.bold { font-weight: \"bold\"}\n", service.getCss());
        assertEquals("div.bold { font-weight: \"bold\"}\n", service.constructedCss);
        assertEquals("div.bold { font-weight: \"bold\"}\n", service.getCss());
    }
}