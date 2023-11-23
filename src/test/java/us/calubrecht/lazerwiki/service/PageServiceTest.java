package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.EntityManagerProxy;
import us.calubrecht.lazerwiki.repository.IdRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;



@SpringBootTest(classes = {PageService.class})
@ActiveProfiles("test")
public class PageServiceTest {

    @Autowired
    PageService pageService;

    @MockBean
    PageRepository pageRepository;

    @MockBean
    IdRepository idRepository;

    @MockBean
    SiteService siteService;

    @MockBean
    NamespaceService namespaceService;

    @MockBean
    EntityManagerProxy em;

    @Test
    public void testExists() {
        assertFalse(pageService.exists("site1","nonExistentPage"));

        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1", "ns", "realPage", false)).
                thenReturn(new Page());
        assertTrue(pageService.exists("host1", "ns:realPage"));
    }

    @Test
    public void getTitle() {
        assertEquals("Non Existent Page", pageService.getTitle("site1", "some:ns:nonExistentPage"));

        Page p = new Page();
        p.setTitle("Titled Page");
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1", "some:ns", "realPage", false)).
                thenReturn(p);
        assertEquals("Titled Page", pageService.getTitle("host1", "some:ns:realPage"));

        Page p2 = new Page();
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1", "some:ns", "nonTitledRealPage", false)).
                thenReturn(p2);
        assertEquals("Non Titled Real Page", pageService.getTitle("host1", "some:ns:nonTitledRealPage"));

        // If no title, then title of "" should be home
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1", "", "", false)).
                thenReturn(p2);
        assertEquals("Home", pageService.getTitle("host1", ""));

        when(siteService.getSiteForHostname(eq("host2"))).thenReturn("site2");
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site2", "", "", false)).
                thenReturn(p);
        assertEquals("Titled Page", pageService.getTitle("host2", ""));
    }

    @Test
    public void testGetPageData() {
        when(siteService.getSiteForHostname(eq("localhost"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("Bob"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("Bob"))).thenReturn(true);
        assertEquals(new PageData("This page doesn't exist", "", false, true, true), pageService.getPageData("localhost", "nonExistantPage", "Bob"));

        Page p = new Page();
        p.setText("This is raw page text");
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1","ns", "realPage", false)).
                thenReturn(p);

        assertEquals(new PageData(null, "This is raw page text", true, true, true), pageService.getPageData("host1", "ns:realPage", "Bob"));

        assertEquals(new PageData( "You are not permissioned to read this page", "",true, false, false), pageService.getPageData("host1", "ns:realPage", "Joe"));


    }

    @Test
    public void testDecodeDescriptor() {
        assertEquals(new PageDescriptor("", "noNS"), pageService.decodeDescriptor("noNS"));
        assertEquals(new PageDescriptor("ns", "withNS"), pageService.decodeDescriptor("ns:withNS"));
        assertEquals("ns:withNS", pageService.decodeDescriptor("ns:withNS").toString());
    }

    @Test
    public void testSavePage() throws PageWriteException {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);

        pageService.savePage("host1", "newPage", "Some text", "someUser");
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository).save(pageCaptor.capture());
        Page p = pageCaptor.getValue();
        assertEquals("Some text", p.getText());
        assertEquals(55L, p.getId());
        assertEquals("site1", p.getSite());
        assertEquals("someUser", p.getModifiedBy());
    }

    @Test
    public void testSavePage_Existing() throws PageWriteException {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        Page p = new Page();
        p.setText("This is raw page text");
        p.setId(10L);
        p.setRevision(2L);
        p.setSite("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1","ns", "realPage", false)).
                thenReturn(p);

        pageService.savePage("host1", "ns:realPage", "Some text", "someUser");
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository, times(2)).save(pageCaptor.capture());

        assertEquals(2, pageCaptor.getAllValues().size());
        Page invalidatedPage = pageCaptor.getAllValues().get(0);
        assertEquals("This is raw page text", invalidatedPage.getText());
        assertEquals(10L, invalidatedPage.getId());
        assertEquals(2L, invalidatedPage.getRevision());
        assertEquals("site1", invalidatedPage.getSite());
        Page newPage = pageCaptor.getAllValues().get(1);
        assertEquals("Some text", newPage.getText());
        assertEquals(10L, newPage.getId());
        assertEquals(3L, newPage.getRevision());
        assertEquals("site1", newPage.getSite());
    }

    @Test
    public void testSavePage_unauthorized()  {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");

        assertThrows(PageWriteException.class, () ->
                pageService.savePage("host1", "newPage", "Some text", "Joe"));
    }

    @Test
    public void testListPages() {
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("joe"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("joe"))).thenReturn(true);
        PageDesc page1 = new PageDescImpl("", "page1", "Page 1", "Bob" );
        PageDesc page2 = new PageDescImpl("", "page2", "Page 1","Francis");
        PageDesc page3 = new PageDescImpl("ns1", "page1", "Page 1","Francis");
        PageDesc page4 = new PageDescImpl("ns1:ns2", "page3", "Page 1","Francis");
        List<PageDesc> allPages = List.of(page1, page2, page3, page4);
        when(namespaceService.filterReadablePages(any(), eq("site1"), eq("joe"))).thenReturn(allPages);


        when(pageRepository.getAllValid("site1")).thenReturn(allPages);

        PageListResponse pageResponse = pageService.getAllPages("host1", "joe");

        assertEquals("", pageResponse.namespaces.getNamespace());
        assertEquals(1, pageResponse.namespaces.getChildren().size());
        assertEquals("ns1", pageResponse.namespaces.getChildren().get(0).getNamespace());
        assertEquals("ns1", pageResponse.namespaces.getChildren().get(0).getFullNamespace());
        NsNode ns1 = pageResponse.namespaces.getChildren().get(0);
        assertEquals(1, ns1.getChildren().size());
        assertEquals("ns2", ns1.getChildren().get(0).getNamespace());
        assertEquals("ns1:ns2", ns1.getChildren().get(0).getFullNamespace());
        assertEquals(0, ns1.getChildren().get(0).getChildren().size());

        Map<String, List<PageDesc>> pages = pageResponse.pages;

        assertEquals(2, pages.get("").size());
        assertEquals("page1", pages.get("").get(0).getPagename());
        assertEquals(1, pages.get("ns1").size());
        assertEquals("page1", pages.get("ns1").get(0).getPagename());
        assertEquals(1, pages.get("ns1:ns2").size());
        assertEquals("page3", pages.get("ns1:ns2").get(0).getPagename());
    }


    @Test
    public void testListPages_wEmptyNamespaces() {
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("joe"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("joe"))).thenReturn(true);
        PageDesc page1 = new PageDescImpl("", "page1", "Page 1", "Bob" );
        PageDesc page2 = new PageDescImpl("ns1:ns2", "page3", "Page 1","Francis");
        List<PageDesc> allPages = List.of(page1, page2);
        when(namespaceService.filterReadablePages(any(), eq("site1"), eq("joe"))).thenReturn(allPages);


        when(pageRepository.getAllValid("site1")).thenReturn(allPages);

        PageListResponse pageResponse = pageService.getAllPages("host1", "joe");
        assertEquals("", pageResponse.namespaces.getNamespace());
        assertEquals(1, pageResponse.namespaces.getChildren().size());
        assertEquals("ns1", pageResponse.namespaces.getChildren().get(0).getNamespace());
        assertEquals("ns1", pageResponse.namespaces.getChildren().get(0).getFullNamespace());
        NsNode ns1 = pageResponse.namespaces.getChildren().get(0);
        assertEquals(1, ns1.getChildren().size());
        assertEquals("ns2", ns1.getChildren().get(0).getNamespace());
        assertEquals("ns1:ns2", ns1.getChildren().get(0).getFullNamespace());
        assertEquals(0, ns1.getChildren().get(0).getChildren().size());

        Map<String, List<PageDesc>> pages = pageResponse.pages;

        assertEquals(1, pages.get("").size());
        assertEquals("page1", pages.get("").get(0).getPagename());
        assertEquals(null, pages.get("ns1"));
        assertEquals(1, pages.get("ns1:ns2").size());
        assertEquals("page3", pages.get("ns1:ns2").get(0).getPagename());
    }


    static class PageDescImpl implements PageDesc {
        String namespace;
        String pageName;
        String title;
        String modifiedBy;

        PageDescImpl(String namespace, String pageName, String title, String modifiedBy) {
            this.namespace = namespace;
            this.pageName = pageName;
            this.title = title;
            this.modifiedBy = modifiedBy;
        }

        @Override
        public String getNamespace() {
            return namespace;
        }

        @Override
        public String getPagename() {
            return pageName;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getModifiedBy() {
            return modifiedBy;
        }

        @Override
        public LocalDateTime getModified() {
            return null;
        }
    }
}
