package us.calubrecht.lazerwiki.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.LazerWikiApplication;
import us.calubrecht.lazerwiki.model.Site;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {LazerWikiApplication.class})
@ActiveProfiles("test")
class SiteRepositoryTest {
    @Autowired
    SiteRepository siteRepository;
    @Test
    void findByHostname() {
        Site defaultSite = siteRepository.findByHostname("*");
        assertEquals("default", defaultSite.name);
        Site site1 = siteRepository.findByHostname("site1.com");
        assertEquals("site1", site1.name);
    }
}