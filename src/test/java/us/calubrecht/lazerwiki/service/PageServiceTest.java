package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.*;
import us.calubrecht.lazerwiki.responses.NsNode;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.responses.PageData.PageFlags;
import us.calubrecht.lazerwiki.responses.PageListResponse;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Collections;

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
    LinkService linkService;

    @MockBean
    PageCacheRepository pageCacheRepository;


    @MockBean
    EntityManagerProxy em;

    @MockBean
    TagRepository tagRepository;

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
        p.setTags(Collections.emptyList());
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1", "some:ns", "realPage", false)).
                thenReturn(p);
        assertEquals("Titled Page", pageService.getTitle("host1", "some:ns:realPage"));

        Page p2 = new Page();
        p2.setTags(Collections.emptyList());
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
        when(namespaceService.canDeleteInNamespace(eq("site1"), any(), eq("Bob"))).thenReturn(true);
        assertEquals(new PageData("This page doesn't exist", "======Non Existant Page======", "Non Existant Page", Collections.emptyList(),Collections.emptyList(), new PageFlags(false, false, true, true, false)),
                pageService.getPageData("localhost", "nonExistantPage", "Bob"));

        Page p = new Page();
        p.setText("This is raw page text");
        p.setTags(Collections.emptyList());
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagename("site1","ns", "realPage")).
                thenReturn(p);

        assertEquals(new PageData(null, "This is raw page text", "Real Page", Collections.emptyList(), Collections.emptyList(), PageData.ALL_RIGHTS), pageService.getPageData("host1", "ns:realPage", "Bob"));

        assertEquals(new PageData( "You are not permissioned to read this page", "", "Real Page", Collections.emptyList(), Collections.emptyList(), PageData.EMPTY_FLAGS), pageService.getPageData("host1", "ns:realPage", "Joe"));


    }

    @Test
    public void testGetPageDataWithBacklinks() {
        when(siteService.getSiteForHostname(eq("localhost"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("Bob"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("Bob"))).thenReturn(true);

        Page p = new Page();
        p.setText("This is raw page text");
        p.setTags(Collections.emptyList());
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagename("site1","ns", "realPage")).
                thenReturn(p);
        when(linkService.getBacklinks("site1", "ns:realPage")).thenReturn(List.of("page1", "page2"));

        assertEquals(new PageData(null, "This is raw page text", "Real Page", Collections.emptyList(), List.of("page1", "page2"), new PageFlags(true, false, true, true, false)), pageService.getPageData("host1", "ns:realPage", "Bob"));
    }

    @Test
    public void testGetPageDataDeleted() {
        when(siteService.getSiteForHostname(eq("localhost"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("Bob"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("Bob"))).thenReturn(true);
        when(namespaceService.canDeleteInNamespace(eq("site1"), any(), eq("Bob"))).thenReturn(true);

        Page p = new Page();
        p.setDeleted(true);
        p.setTags(Collections.emptyList());
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagename("site1","", "deletedPage")).
                thenReturn(p);

        assertEquals(new PageData("This page doesn't exist", "======Deleted Page======", "Deleted Page", Collections.emptyList(), Collections.emptyList(), new PageFlags(false, true, true, true, false)), pageService.getPageData("host1", "deletedPage", "Bob"));


    }
    @Test
    public void testGetPageDataHome() {
        when(siteService.getSiteForHostname(eq("localhost"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("Bob"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("Bob"))).thenReturn(true);
        when(namespaceService.canDeleteInNamespace(eq("site1"), any(), eq("Bob"))).thenReturn(true);

        Page p = new Page();
        p.setText("Hi");
        p.setTags(Collections.emptyList());
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagename("site1","", "")).
                thenReturn(p);

        assertEquals(new PageData(null, "Hi", "Home", Collections.emptyList(), Collections.emptyList(), new PageFlags(true, false, true, true, false)), pageService.getPageData("host1", "", "Bob"));


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

        pageService.savePage("host1", "newPage", "Some text", Collections.emptyList(),  Collections.emptyList(),"Title","someUser");
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository).save(pageCaptor.capture());
        Page p = pageCaptor.getValue();
        p.setTags(Collections.emptyList());
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
        p.setTitle("Title");
        p.setId(10L);
        p.setRevision(2L);
        p.setSite("site1");
        p.setTags(Collections.emptyList());
        when(pageRepository.getBySiteAndNamespaceAndPagename("site1","ns", "realPage")).
                thenReturn(p);

        pageService.savePage("host1", "ns:realPage", "Some text", Collections.emptyList(),  Collections.emptyList(),"Title","someUser");
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
                pageService.savePage("host1", "newPage", "Some text", null,  Collections.emptyList(),"Title","Joe"));
    }

    @Test
    public void testSavePageWithLinks() throws PageWriteException {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);

        pageService.savePage("host1", "newPage", "Some text", Collections.emptyList(),  List.of("page1", "page2"),"Title","someUser");

        verify(linkService).setLinksFromPage("site1", "", "newPage",  List.of("page1", "page2"));
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

        List<String> pageList = pageService.getAllPagesFlat("host1", "joe");
        assertEquals(List.of("page1","page2", "ns1:page1", "ns1:ns2:page3"), pageList);
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

    @Test
    public void testGetAllTags() {
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(tagRepository.getAllActiveTags("site1")).thenReturn(List.of("tag1", "tag2"));
        assertEquals(2, pageService.getAllTags("host1", "joe").size());
    }

    @Test
    public void testSearchTag() {
        PageDesc page1 = new PageDescImpl("", "page1", "Page 1", "Bob" );
        PageDesc page2 = new PageDescImpl("ns1:ns2", "page3", "Page 1","Francis");
        List<PageDesc> pages = List.of(page1, page2);
        when(pageRepository.getByTagname("site1", "tag1")).thenReturn(pages);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.filterReadablePages(any(), eq("site1"), eq("bob"))).thenReturn(pages);
        when(namespaceService.filterReadablePages(any(), eq("site1"), eq("joe"))).thenReturn(List.of(page1));

        List<PageDesc> results = pageService.searchPages("host1", "bob","tag:tag1");
        assertEquals(2, results.size());

        results = pageService.searchPages("host1", "joe","tag:tag1");
        assertEquals(1, results.size());

        Map<String, String> searchTerms = Map.of("tag","tag1", "ns", "ns1:ns2");
        results = pageService.searchPages("host1", "bob",searchTerms);
        assertEquals(1, results.size());
    }

    @Test
    public void testUnsupportedSearch() {
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        Map<String, String> searchTerms = Map.of("fullText","Test");
        List<PageDesc> results = pageService.searchPages("host1", "bob",searchTerms);
        assertEquals(0, results.size());

         verify(pageRepository, never()).getByTagname(anyString(), anyString());
    }

    @Test
    public void testDeletePage() throws PageWriteException {
        when(siteService.getSiteForHostname(eq("localhost"))).thenReturn("default");
        when(namespaceService.canDeleteInNamespace(eq("default"), eq(""), eq("bob"))).thenReturn(true);
        Page p = new Page();
        p.setId(1000L);
        p.setPagename("testPage");
        p.setNamespace("");
        p.setDeleted(false);
        p.setRevision(10L);
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(eq("default"), eq(""), eq("testPage"), eq(false))).thenReturn(p);

        assertThrows(PageWriteException.class, ()-> pageService.deletePage("localhost", "testPage", "frank"));

        assertThrows(PageWriteException.class, ()-> pageService.deletePage("localhost", "", "bob"));

        verify(linkService, never()).deleteLinks(anyString(), anyString());

        pageService.deletePage("localhost", "unknownPage", "bob");
        verify(linkService, never()).deleteLinks(anyString(), anyString());

        pageService.deletePage("localhost", "testPage", "bob");
        verify(linkService).deleteLinks(eq("default"), eq("testPage"));
        ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository, times(2)).save(captor.capture());

        assertEquals(2, captor.getAllValues().size());
        Page invalidatedPage = captor.getAllValues().get(0);
        assertEquals(1000L, invalidatedPage.getId());
        assertEquals(10L, invalidatedPage.getRevision());
        Page newPage = captor.getAllValues().get(1);
        assertEquals("", newPage.getText());
        assertEquals(1000L, newPage.getId());
        assertEquals(11L, newPage.getRevision());
        assertTrue(newPage.isDeleted());
    }

    @Test
    public void testGetTemplate() {
        Page ns1Template = new Page();
        ns1Template.setText("== Page: %NAME%==\nIn %NAMESPACE%");
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site", "ns1", "_template", false)).thenReturn(ns1Template);
        when(namespaceService.parentNamespace("ns1:ns2")).thenReturn("ns1");
        when(namespaceService.parentNamespace("ns1")).thenReturn("");
        when(namespaceService.parentNamespace("ns3")).thenReturn("");
        assertEquals("== Page: Page 1==\nIn ns1", pageService.getTemplate("site", new PageDescriptor("ns1", "page1")));
        assertEquals("== Page: Page 2==\nIn ns1:ns2", pageService.getTemplate("site", new PageDescriptor("ns1:ns2", "page2")));
        assertEquals("======Page 3======", pageService.getTemplate("site", new PageDescriptor("ns3", "page3")));
        assertEquals("======Page 4======", pageService.getTemplate("site", new PageDescriptor("", "page4")));
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

        PageDescImpl(String namespace, String pageName) {
            this.namespace = namespace;
            this.pageName = pageName;
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
