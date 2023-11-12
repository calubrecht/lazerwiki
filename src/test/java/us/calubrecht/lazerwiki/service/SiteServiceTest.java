package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = SiteService.class)
@ActiveProfiles("test")
public class SiteServiceTest {

    @Autowired
    SiteService underTest;

    @Test
    public void test() {
        assertEquals("default", underTest.getSiteForHostname("anyHost"));
    }
}
