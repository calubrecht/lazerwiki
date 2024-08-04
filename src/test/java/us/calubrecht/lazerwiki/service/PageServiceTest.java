package us.calubrecht.lazerwiki.service;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import org.apache.commons.lang3.builder.ToStringExclude;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.*;
import us.calubrecht.lazerwiki.responses.NsNode;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.responses.PageData.PageFlags;
import us.calubrecht.lazerwiki.responses.PageListResponse;
import us.calubrecht.lazerwiki.responses.SearchResult;
import us.calubrecht.lazerwiki.service.exception.PageReadException;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;


import java.time.LocalDateTime;
import java.util.*;

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

        PageDescriptor pd = new PageDescriptor("ns", "page_name");
        assertEquals("Page Name", pageService.getTitle(pd, null));
        Page pageNoTitle = new Page();
        assertEquals("Page Name", pageService.getTitle(pd, pageNoTitle));
        Page pageWithTitle = new Page();
        pageWithTitle.setTitle("Other Name");
        assertEquals("Other Name", pageService.getTitle(pd, pageWithTitle));
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
        when(namespaceService.filterReadablePageDescriptors(any(), any(), any())).thenReturn(
                List.of(new PageDescriptor("", "page1"), new PageDescriptor("", "page2"))
        );

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
        p.setId(1L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagename("site1","", "")).
                thenReturn(p);

        assertEquals(new PageData(null, "Hi", "Home", Collections.emptyList(), Collections.emptyList(), new PageFlags(true, false, true, true, false), 1L), pageService.getPageData("host1", "", "Bob"));


    }

    @Test
    public void testDecodeDescriptor() {
        assertEquals(new PageDescriptor("", "noNS"), PageService.decodeDescriptor("noNS"));
        assertEquals(new PageDescriptor("ns", "withNS"), PageService.decodeDescriptor("ns:withNS"));
        assertEquals("ns:withNS", PageService.decodeDescriptor("ns:withNS").toString());
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
        assertNull(pages.get("ns1"));
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

        List<SearchResult> results = pageService.searchPages("host1", "bob","tag:tag1").get("tag");
        assertEquals(2, results.size());

        results = pageService.searchPages("host1", "joe","tag:tag1").get("tag");
        assertEquals(1, results.size());

        Map<String, String> searchTerms = Map.of("tag","tag1", "ns", "ns1:ns2");
        results = pageService.searchPages("host1", "bob",searchTerms).get("tag");
        assertEquals(1, results.size());
    }

    @Test
        public void testSearchText() {
        PageCache page1 = new PageCache("site1", "", "page1", "Page 1", "", "This is a page\nWith some bananas\nAnd a cow", false);
        PageCache page2 = new PageCache("site1", "ns", "page2", "Page2", "", "All your bananas\nbelong to me\nThe banana thief", false);
        List<PageCache> pages = List.of(page1, page2);
        List<PageDesc> pagesDesc = List.of(page1, page2);
        when(pageCacheRepository.searchByTitle(eq("site1"), eq("banana*"))).thenReturn(List.of(page1, page2));
        when(pageCacheRepository.searchByText(eq("site1"), eq("banana*"))).thenReturn(List.of(page1, page2));
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.filterReadablePages(any(), eq("site1"), eq("bob"))).thenReturn(pagesDesc);

        Map<String, List<SearchResult>> results = pageService.searchPages("host1", "bob","text:banana");
        assertEquals(2, results.get("title").size());
        assertEquals(2, results.get("text").size());

        assertEquals("With some bananas", results.get("text").get(0).resultLine());
        assertEquals("All your bananas", results.get("text").get(1).resultLine());

        results = pageService.searchPages("host1", "bob",Map.of("text","banana", "ns", "ns"));
        assertEquals(1, results.get("title").size());
        assertEquals(1, results.get("text").size());

        // If asterix specifically applied
        results = pageService.searchPages("host1", "bob","text:banana*");
        assertEquals(2, results.get("title").size());
        assertEquals(2, results.get("text").size());

    }

    @Test
    public void testUnsupportedSearch() {
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        Map<String, String> searchTerms = Map.of("fullText","Test");
        Map<String, List<SearchResult>> results = pageService.searchPages("host1", "bob",searchTerms);
        assertEquals(0, results.size());

         verify(pageRepository, never()).getByTagname(anyString(), anyString());
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

    @Test
    public void testGetCachedPage() {
        when(siteService.getSiteForHostname("localhost")).thenReturn("default");
        PageCache cached = new PageCache();
        cached.renderedCache = "Rendered";
        PageCache.PageCacheKey key = new PageCache.PageCacheKey("default", "ns", "cached");
        when(pageCacheRepository.findById(key)).thenReturn(Optional.of(cached));
        PageCache ret = pageService.getCachedPage("localhost", "ns:cached");
        assertEquals("Rendered", ret.renderedCache);
    }

    @Test
    public void testGetCachedPages() {
        when(siteService.getSiteForHostname("localhost")).thenReturn("default");
        PageCache cache1 = new PageCache();
        cache1.renderedCache = "Text 1";
        PageCache cache2 = new PageCache();
        cache2.renderedCache = "Text 2";
        ArgumentCaptor<List<PageCache.PageCacheKey>> captor = ArgumentCaptor.forClass(List.class);
        when(pageCacheRepository.findAllById(captor.capture())).thenReturn(List.of(cache1, cache2));
        List<PageCache> ret = pageService.getCachedPages("localhost", List.of("page1", "ns:page2"));
        assertEquals(2, ret.size());

        List<PageCache.PageCacheKey> arg = captor.getValue();
        assertEquals("default", arg.get(0).site);
        assertEquals("", arg.get(0).namespace);
        assertEquals("page1", arg.get(0).pageName);
        assertEquals("ns", arg.get(1).namespace);
        assertEquals("page2", arg.get(1).pageName);
    }

    @Test
    public void testSavePage() {
        when(siteService.getSiteForHostname("localhost")).thenReturn("default");
        RenderResult rendered = new RenderResult("rendered", "notRendered", Map.of("DONT_CACHE", true));
        Page p = new Page();
        p.setPagename("TOCACHE");
        p.setNamespace("ns");
        when(pageRepository.getBySiteAndNamespaceAndPagename("default", "ns", "toCache")).thenReturn(p);
        pageService.saveCache("localhost", "ns:toCache", rendered);
        PageCache cached = new PageCache();
        cached.site = "default";
        cached.namespace = "ns";
        cached.pageName= "TOCACHE"; // Use name with case as set in the database
        cached.renderedCache = "rendered";
        cached.plaintextCache = "notRendered";
        cached.useCache = false;

        verify(pageCacheRepository).save(cached);

        // Save and Cache
        RenderResult rendered2 = new RenderResult("rendered", "notRendered", new HashMap<>());
        when(pageRepository.getBySiteAndNamespaceAndPagename("default", "ns", "toCache")).thenReturn(p);
        pageService.saveCache("localhost", "ns:toCache", rendered2);
        PageCache cached2 = new PageCache();
        cached2.site = "default";
        cached2.namespace = "ns";
        cached2.pageName= "TOCACHE"; // Use name with case as set in the database
        cached2.renderedCache = "rendered";
        cached2.plaintextCache = "notRendered";
        cached2.useCache = true;
        verify(pageCacheRepository).save(cached2);
    }

    @Test
    public void testHistoricalPageData() {
        when(siteService.getSiteForHostname("localhost")).thenReturn("default");
        when(namespaceService.canReadNamespace(eq("default"), any(), eq("bob"))).thenReturn(true);

        PageData pd = pageService.getHistoricalPageData("localhost", "page1", 1, "joe");
        // Joe can't read this page
        assertEquals("You are not permissioned to read this page", pd.rendered());
        assertEquals(false, pd.flags().userCanRead());

        Page page = new Page();
        page.setText("This is a page");
        page.setTags(Collections.emptyList());
        Page deletedPage = new Page();
        deletedPage.setDeleted(true);
        deletedPage.setTags(Collections.emptyList());
        when(pageRepository.findBySiteAndNamespaceAndPagenameAndRevision("default", "", "page1", 1)).thenReturn(page);
        when(pageRepository.findBySiteAndNamespaceAndPagenameAndRevision("default", "", "page1", 4)).thenReturn(deletedPage);
        pd = pageService.getHistoricalPageData("localhost", "page1", 123, "bob");
        assertEquals("This page doesn't exist", pd.rendered());
        pd = pageService.getHistoricalPageData("localhost", "page1", 4, "bob");
        assertEquals("This page doesn't exist", pd.rendered());
        assertEquals(true, pd.flags().wasDeleted());
        pd = pageService.getHistoricalPageData("localhost", "page1", 1, "bob");
        assertEquals("This is a page", pd.source());

    }

    @Test
    void testGetPageHistory() throws PageReadException {
        when(siteService.getSiteForHostname("localhost")).thenReturn("default");
        PageDesc v1 = new PageDescImpl("ns", "page1", 1L);
        PageDesc v2 = new PageDescImpl("ns", "page1", 1L);
        PageDesc v3 = new PageDescImpl("ns", "page1", 3L);
        when(pageRepository.findAllBySiteAndNamespaceAndPagenameOrderByRevision("default", "ns", "page1")).thenReturn(List.of(v1, v2, v3));
        when(namespaceService.canReadNamespace(eq("default"), any(), eq("Bob"))).thenReturn(true);

        assertEquals(3, pageService.getPageHistory("localhost", "ns:page1", "Bob").size());
        assertThrows(PageReadException.class, () -> pageService.getPageHistory("localhost", "ns:page1", "Frank"));

    }

    @Test
    void testGetPageDiff() throws PageReadException {
        when(siteService.getSiteForHostname("localhost")).thenReturn("default");
        when(namespaceService.canReadNamespace(eq("default"), any(), eq("bob"))).thenReturn(true);
        Page page = new Page();
        page.setText("THis is\ntext");
        PageKey key1 = new PageKey(5L, 1L);
        when(pageRepository.findById(key1)).thenReturn(Optional.of(page));
        Page page2 = new Page();
        page2.setText("THis is\nmore text");
        PageKey key2 = new PageKey(5L, 5L);
        when(pageRepository.findById(key2)).thenReturn(Optional.of(page2));
        Page latestPage = new Page();
        latestPage.setId(5L);
        when(pageRepository.getBySiteAndNamespaceAndPagename("default", "", "thisPage")).thenReturn(latestPage);

        // Joe can't read
        assertThrows(PageReadException.class, () -> pageService.getPageDiff("localhost", "thisPage", 1L, 2L, "joe"));
        // These revisions don't exist
        assertThrows(PageReadException.class, () -> pageService.getPageDiff("localhost", "thisPage", 8L, 20L, "bob"));
        assertThrows(PageReadException.class, () -> pageService.getPageDiff("localhost", "thisPage", 1L, 20L, "bob"));

        List<Pair<Integer, String>> out = pageService.getPageDiff("localhost", "thisPage", 1L, 5L, "bob");

        assertEquals(2, out.size());
        assertEquals(1, out.get(0).getFirst());
        assertEquals("THis is", out.get(0).getSecond());
        assertEquals("<span class=\"editNewInline\">more </span>text", out.get(1).getSecond());

    }

    @Test
    public void testGenerateDiffs() {
        List<Pair<Integer, String>> out = pageService.generateDiffs("This is a line\nAnd another", "This is a line\nWith an extra one\nAnd another");
        assertEquals(3, out.size());
        assertEquals(-1, out.get(1).getFirst());
        assertEquals("<span class=\"editNewInline\">With an extra one</span>", out.get(1).getSecond());
        assertEquals(2, out.get(2).getFirst());
    }

    @Test
    public void testGetPageData_bulk() {
        when(siteService.getSiteForHostname(eq("localhost"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("Bob"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("Bob"))).thenReturn(true);
        when(namespaceService.canDeleteInNamespace(eq("site1"), any(), eq("Bob"))).thenReturn(true);

        Page p = new Page();
        p.setText("This is raw page text");
        p.setTags(Collections.emptyList());
        PageText pt1 = new PageTextImpl("", "page1", null, "text");
        PageText pt2 = new PageTextImpl("ns_secret", "secret", "","text");
        PageText pt3 = new PageTextImpl("", "", "Home Page","text_home");
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        when(pageRepository.getAllBySiteAndNamespaceAndPagename(eq("site1"), captor.capture())).
                thenReturn(List.of(pt1, pt2, pt3));

        Map<PageDescriptor, PageData> res = pageService.getPageData("localhost", List.of("page1", "ns:notPage", "ns_secret:secret", ""), "Bob");

        assertEquals(":page1", captor.getValue().get(0));
        assertEquals("ns:notPage", captor.getValue().get(1));

        assertEquals("text", res.get(new PageDescriptor("", "page1")).source());
        assertFalse(res.containsKey(new PageDescriptor("ns", "notPage")));
        assertEquals("text", res.get(new PageDescriptor("ns_secret", "secret")).source());
        assertFalse(res.get(new PageDescriptor("", "")).flags().userCanDelete());

        when(namespaceService.canReadNamespace(eq("site1"), eq(""), eq("Frank"))).thenReturn(true);
        when(namespaceService.canReadNamespace(eq("site1"), eq("ns_secret"), eq("Frank"))).thenReturn(false);
        res = pageService.getPageData("localhost", List.of("page1", "ns:notPage", "ns_secret:secret", ""), "Frank");

        assertEquals("text", res.get(new PageDescriptor("", "page1")).source());
        assertEquals("page1", res.get(new PageDescriptor("", "page1")).title());
        assertEquals("", res.get(new PageDescriptor("ns_secret", "secret")).source());
        assertEquals("You are not permissioned to read this page", res.get(new PageDescriptor("ns_secret", "secret")).rendered());
        assertEquals("Home Page", res.get(new PageDescriptor("", "")).title());

    }

    @Test
    void recentChanges() {
        when(siteService.getSiteForHostname(eq("theHost"))).thenReturn("site1");
        when(pageRepository.findAllBySiteAndNamespaceInOrderByModifiedDesc(any(), eq("site1"), any())).thenReturn(
                List.of(new PageDescImpl("ns", "page1", 2L), new PageDescImpl("ns", "page2", 1L),
                        new PageDescImpl("ns", "page4", 3L,  true))
        );
        RecentChangesResponse changes = pageService.recentChanges("theHost", "Bob");

        assertEquals(3, changes.changes().size());
        RecentChangesResponse.RecentChangeRec rec1 = changes.changes().get(0);
        assertEquals("page1", rec1.pageDesc().getPagename());
        assertEquals("Modified", rec1.action());
        RecentChangesResponse.RecentChangeRec rec2 = changes.changes().get(1);
        assertEquals("page2", rec2.pageDesc().getPagename());
        assertEquals("Created", rec2.action());
        RecentChangesResponse.RecentChangeRec rec3 = changes.changes().get(2);
        assertEquals("page4", rec3.pageDesc().getPagename());
        assertEquals("Deleted", rec3.action());
    }


    public static class PageDescImpl implements PageDesc {
        final String namespace;
        final String pageName;
        String title;
        String modifiedBy;
        LocalDateTime modified;

        Long revision;
        boolean deleted = false;

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

        PageDescImpl(String namespace, String pageName, Long revision, boolean deleted) {
            this.namespace = namespace;
            this.pageName = pageName;
            this.deleted = deleted;
            this.revision = revision;
        }
        public PageDescImpl(String namespace, String pageName, Long revision) {
            this.namespace = namespace;
            this.pageName = pageName;
            this.revision = revision;
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
            return modified;
        }

        public void setModified(LocalDateTime date) {
            modified = date;}

        @Override
        public boolean isDeleted() {
            return deleted;
        }

        @Override
        public Long getRevision() {return revision;}
    }

    public static class PageTextImpl implements PageText {
        String namespace;
        String pagename;
        String title;
        String text;

        public PageTextImpl(String namespace, String pagename, String title, String text) {
            this.namespace = namespace;
            this.pagename = pagename;
            this.title = title;
            this.text = text;
        }
        @Override
        public String getNamespace() {
            return namespace;
        }

        @Override
        public String getPagename() {
            return pagename;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getText() {
            return text;
        }
    }

}
