package us.calubrecht.lazerwiki.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Limit;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.LazerWikiApplication;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.model.PageText;
import us.calubrecht.lazerwiki.util.DbSupport;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Page site = pageRepository.findBySiteAndNamespaceAndPagenameAndValidts("site1", "ns", "deletedPage", MAX_DATE, Page.class);
        assertNotNull(site);
    }


    @Test
    void getBySiteAndNamespaceAndPagenameAndDeleted() {
        Page site = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1", "ns", "page1", false);
        assertNotNull(site);
    }

    @Test
    void getBySiteAndNamespaceAndPagename() {
        Page site = pageRepository.getBySiteAndNamespaceAndPagename("site1", "ns", "page1");
        assertNotNull(site);
    }

    @Test
    void getLastRevisionBySiteAndNamespaceAndPagename() {
        Long rev = pageRepository.getLastRevisionBySiteAndNamespaceAndPagename("site1", "ns", "page1");
        assertEquals(2L, rev);

        rev = pageRepository.getLastRevisionBySiteAndNamespaceAndPagename("site1", "ns", "nonexistantPage");
        assertEquals(null, rev);

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
        List<PageText> pages = pageRepository.getAllBySiteAndNamespaceAndPagename("mysql","site1", List.of("ns:page1", "ns:page2"));
        assertEquals(2, pages.size());
        assertEquals("page1", pages.get(0).getPagename());
        assertEquals("some text", pages.get(0).getText());
        assertEquals("page2", pages.get(1).getPagename());
        assertEquals("othertext", pages.get(1).getText());
    }

    @Test
    void findAllBySiteOrderByModifiedDesc() {
        List<PageDesc> pageDescs = pageRepository.findAllBySiteAndNamespaceInOrderByModifiedDesc(Limit.of(10), "site1", List.of("ns"));
        assertEquals(5, pageDescs.size());

        assertEquals("ns:page2#1", pageDescs.get(0).getDescriptor() + "#" + pageDescs.get(0).getRevision());
        assertEquals("ns:deletedPage#2", pageDescs.get(1).getDescriptor() + "#" + pageDescs.get(1).getRevision());
        assertTrue(pageDescs.get(1).isDeleted());
        assertEquals("ns:deletedPage#1", pageDescs.get(2).getDescriptor() + "#" + pageDescs.get(2).getRevision());
        assertFalse(pageDescs.get(2).isDeleted());
    }

    @Test
    void getAllActiveNamespaces() {
        List<String> namespaces = pageRepository.getAllNamespaces("site2");

        assertEquals(2, namespaces.size());
        assertEquals(Set.of("ns2", "ns5"), new HashSet<>(namespaces));
    }

    @Test
    void test_getByTagname() {
        List<PageDesc> tags = pageRepository.getByTagname("mysql", "site2", "bigTag");

        assertEquals(1, tags.size());
        assertEquals("pagens2a", tags.get(0).getPagename());

    }

    @Test
    void testGetMaxTS() {
        assertEquals("9999-12-31 00:00:00", pageRepository.getMaxTS("mysql"));
        assertEquals("253402232400000", pageRepository.getMaxTS("sqlite"));
    }
}