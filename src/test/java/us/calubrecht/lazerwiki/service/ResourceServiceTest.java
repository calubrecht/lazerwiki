package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {ResourceService.class})
@ActiveProfiles("test")
class ResourceServiceTest {

    @Autowired
    ResourceService underTest;

    @MockBean
    SiteService siteService;

    @Test
    void getBinaryFile() throws IOException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        byte[] bytes = underTest.getBinaryFile("localhost", "bluecircle.png");
        assertTrue(bytes != null);
        assertEquals(768, bytes.length);

        // File from src/main/resources/static
        bytes = underTest.getBinaryFile("localhost", "site.css");
        assertTrue(bytes != null);
        assertEquals("/* Site specific css to override defaults */", new String(bytes).trim());
    }
}