package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import us.calubrecht.lazerwiki.model.PageCache;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.repository.PageCacheRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;
import us.calubrecht.lazerwiki.responses.SearchResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {PageSearchService.class})
@ActiveProfiles("test")
public class PageSearchServiceTest {
    @Autowired
    PageSearchService pageSearchService;

    @MockitoBean
    PageRepository pageRepository;

    @MockitoBean
    SiteService siteService;

    @MockitoBean
    NamespaceService namespaceService;

    @MockitoBean
    PageCacheRepository pageCacheRepository;


    @Test
    public void testSearchTag() {
        PageDesc page1 = new PageServiceTest.PageDescImpl("", "page1", "Page 1", "Bob" );
        PageDesc page2 = new PageServiceTest.PageDescImpl("ns1:ns2", "page3", "Page 1","Francis");
        List<PageDesc> pages = List.of(page1, page2);
        when(pageRepository.getByTagname("mysql","site1", "tag1")).thenReturn(pages);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.filterReadablePages(any(), eq("site1"), eq("bob"))).thenReturn(pages);
        when(namespaceService.filterReadablePages(any(), eq("site1"), eq("joe"))).thenReturn(List.of(page1));

        List<SearchResult> results = pageSearchService.searchPages("host1", "bob","tag:tag1").get("tag");
        assertEquals(2, results.size());

        results = pageSearchService.searchPages("host1", "joe","tag:tag1").get("tag");
        assertEquals(1, results.size());

        Map<String, String> searchTerms = Map.of("tag","tag1", "ns", "ns1:ns2");
        results = pageSearchService.searchPages("host1", "bob",searchTerms).get("tag");
        assertEquals(1, results.size());
    }


    @Test
    public void testSearchText() {
        PageCache page1 = new PageCache("site1", "", "page1", "Page 1", "", "This is a page\nWith some bananas\nAnd a cow", false);
        PageCache page2 = new PageCache("site1", "ns", "page2", "Page2", "", "All your bananas\nbelong to me\nThe banana thief", false);
        List<PageCache> pages = List.of(page1, page2);
        List<PageDesc> pagesDesc = List.of(page1, page2);
        when(pageCacheRepository.searchByTitle(any(), eq("site1"), eq("banana*"))).thenReturn(List.of(page1, page2));
        when(pageCacheRepository.searchByText(any(),eq("site1"), eq("banana*"))).thenReturn(List.of(page1, page2));
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.filterReadablePages(any(), eq("site1"), eq("bob"))).thenReturn(pagesDesc);

        Map<String, List<SearchResult>> results = pageSearchService.searchPages("host1", "bob","text:banana");
        assertEquals(2, results.get("title").size());
        assertEquals(2, results.get("text").size());

        assertEquals("With some bananas", results.get("text").get(0).resultLine());
        assertEquals("All your bananas", results.get("text").get(1).resultLine());

        results = pageSearchService.searchPages("host1", "bob",Map.of("text","banana", "ns", "ns"));
        assertEquals(1, results.get("title").size());
        assertEquals(1, results.get("text").size());
        assertEquals("All your bananas", results.get("text").get(0).resultLine());

        // If asterix specifically applied
        results = pageSearchService.searchPages("host1", "bob","text:banana*");
        assertEquals(2, results.get("title").size());
        assertEquals(2, results.get("text").size());

        // If full search term exists
        results = pageSearchService.searchPages("host1", "bob", "text:banana thief");
        assertEquals(2, results.get("title").size());
        assertEquals(2, results.get("text").size());
        assertEquals("The banana thief", results.get("text").get(1).resultLine());

        PageCache page3 = new PageCache("site1", "ns", "page2", "Page2", "", "The dog is there\nall the dogs are me\nThe cow lives", false);
        when(namespaceService.filterReadablePages(any(), eq("site1"), eq("jay"))).thenReturn(List.of(page3));
        when(pageCacheRepository.searchByText(any(),eq("site1"), eq("the cow"))).thenReturn(List.of(page3));
        results = pageSearchService.searchPages("host1", "jay", "text:the cow");
        assertEquals(1, results.get("title").size());
        assertEquals(1, results.get("text").size());
        assertEquals("The cow lives", results.get("text").get(0).resultLine());
    }

    @Test
    public void testSearchTextDeprioritizeThe() {
        PageCache page1 = new PageCache("site1", "", "page1", "Page 1", "", "The rock is old\nThe rock is very old\nmight be a granite", false);
        List<PageCache> pages = List.of(page1);
        List<PageDesc> pagesDesc = List.of(page1);
        when(pageCacheRepository.searchByTitle(any(), any(), any())).thenReturn(List.of());
        when(pageCacheRepository.searchByText(any(), any(), any())).thenReturn(pages);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.filterReadablePages(any(), eq("site1"), eq("bob"))).thenAnswer(inv ->
        {
            return (List<PageDesc>)inv.getArgument(0);
        });

        Map<String, List<SearchResult>> results = pageSearchService.searchPages("host1", "bob","text:The granite");
        assertEquals(0, results.get("title").size());
        assertEquals(1, results.get("text").size());
        assertEquals("might be a granite", results.get("text").get(0).resultLine());

        results = pageSearchService.searchPages("host1", "bob","text:The");
        assertEquals(1, results.get("text").size());
        assertEquals("The rock is old", results.get("text").get(0).resultLine());

        results = pageSearchService.searchPages("host1", "bob","text:The cowboy");
        assertEquals(1, results.get("text").size());
        assertEquals("The rock is old", results.get("text").get(0).resultLine());

        results = pageSearchService.searchPages("host1", "bob","text:The cowboy");
        assertEquals(1, results.get("text").size());
        assertEquals("The rock is old", results.get("text").get(0).resultLine());

        pageSearchService.searchPages("host1", "bob","text:The sword");
        verify(pageCacheRepository).searchByTitle(any(), any(), eq("sword*"));

        verify(pageCacheRepository, times(1)).searchByTitle(any(), any(), eq("The*"));
        pageSearchService.searchPages("host1", "bob","text:The");
        verify(pageCacheRepository, times(2)).searchByTitle(any(), any(), eq("The*"));
    }

    @Test
    public void testUnsupportedSearch() {
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        Map<String, String> searchTerms = Map.of("fullText","Test");
        Map<String, List<SearchResult>> results = pageSearchService.searchPages("host1", "bob",searchTerms);
        assertEquals(0, results.size());

        verify(pageRepository, never()).getByTagname(anyString(), anyString(), anyString());
    }

}
