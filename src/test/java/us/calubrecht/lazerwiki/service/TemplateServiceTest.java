package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TemplateService.class)
@ActiveProfiles("test")
class TemplateServiceTest {

    @Autowired
    TemplateService underTest;

    @Test
    void getVerifyEmailTemplate() {
        String res = underTest.getVerifyEmailTemplate("Site1", "bob@super.com", "Bob", "ABC");
        assertTrue(res.contains("Site1"));
        assertTrue(res.contains("bob@super.com"));
        assertTrue(res.contains("Bob"));
        assertTrue(res.contains("ABC"));
    }
}