package us.calubrecht.lazerwiki.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.LazerWikiApplication;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageDesc;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static us.calubrecht.lazerwiki.repository.PageRepository.MAX_DATE;

@SpringBootTest(classes = {LazerWikiApplication.class})
@ActiveProfiles("test")
class PageRepositoryTest {

    @Autowired
    PageRepository pageRepository;

    @Test
    void findBySiteAndNamespaceAndPagenameAndValidtsAndDeleted() {
        Page site = pageRepository.findBySiteAndNamespaceAndPagenameAndValidtsAndDeleted("site1", "ns", "page1", MAX_DATE, false);
        assertNotNull(site);
    }

    @Test
    void getBySiteAndNamespaceAndPagenameAndDeleted() {
        Page site = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1", "ns", "page1", false);
        assertNotNull(site);
    }

    @Test
    void getAllValid() {
        List<PageDesc> pages = pageRepository.getAllValid("site1");
        assertEquals(1, pages.size());
    }
}