package us.calubrecht.lazerwiki.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.LazerWikiApplication;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.model.PageText;
import us.calubrecht.lazerwiki.util.DbSupport;

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

    DbSupport dbSupport = new DbSupport(); // Does nothing but satisfy coverage. Constructor for utility class with only static methods

    @Test
    void findBySiteAndNamespaceAndPagenameAndValidtsAndDeleted() {
        Page site = pageRepository.findBySiteAndNamespaceAndPagenameAndValidtsAndDeleted("site1", "ns", "page1", MAX_DATE, false);
        assertNotNull(site);
    }

    @Test
    void findBySiteAndNamespaceAndPagenameAndValidts() {
        Page site = pageRepository.findBySiteAndNamespaceAndPagenameAndValidts("site1", "ns", "deletedPage", MAX_DATE);
        assertNotNull(site);
    }

    @Test
    void getBySiteAndNamespaceAndPagenameAndDeleted() {
        Page site = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1", "ns", "page1", false);
        assertNotNull(site);
    }

    @Test
    void getBySiteAndNamespaceAndPagenameAndValidts() {
        Page site = pageRepository.getBySiteAndNamespaceAndPagename("site1", "ns", "deletedPage");
        assertNotNull(site);
    }

    @Test
    void getAllValid() {
        List<PageDesc> pages = pageRepository.getAllValid("site1");
        assertEquals(2, pages.size());
    }

    @Test
    void findAllBySiteAndNamespaceAndPagenameOrderByRevision() {
        List<PageDesc> pageDescs = pageRepository.findAllBySiteAndNamespaceAndPagenameOrderByRevision("site1", "ns", "page1");
        assertEquals(2, pageDescs.size());

    }

    @Test
    void getAllBySiteAndNamespaceAndPagename() {
        List<PageText> pages = pageRepository.getAllBySiteAndNamespaceAndPagename("site1", List.of("ns:page1", "ns:page2"));
        assertEquals(2, pages.size());
        assertEquals("page1", pages.get(0).getPagename());
        assertEquals("some text", pages.get(0).getText());
        assertEquals("page2", pages.get(1).getPagename());
        assertEquals("othertext", pages.get(1).getText());
    }
}