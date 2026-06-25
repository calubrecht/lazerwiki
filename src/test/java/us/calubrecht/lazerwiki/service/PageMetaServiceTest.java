package us.calubrecht.lazerwiki.service;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageCache;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.repository.EntityManagerProxy;
import us.calubrecht.lazerwiki.repository.PageCacheRepository;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PageMetaServiceTest {

    @InjectMocks
    PageMetaService underTest;

    @Mock
    LinkService linkService;

    @Mock
    LinkOverrideService linkOverrideService;

    @Mock
    MediaOverrideService mediaOverrideService;

    @Mock
    ImageRefService imageRefService;

    @Mock
    RegenCacheService regenCacheService;

    @Mock
    PageCacheRepository pageCacheRepository;

    @Mock
    EntityManagerProxy em;

    @Test
    void test_updateMetaData() {
        // null page = new page
        underTest.updateMetaData("host1", "site1", PageDescriptor.fromFullName("ns1:page1"), null, List.of("page4", "page5"), List.of("img1", "img2"));
        verify(linkOverrideService).deleteOverrides("host1", "ns1:page1");
        verify(mediaOverrideService).deleteOverrides("host1", "ns1:page1");
        verify(linkService).setLinksFromPage("site1", "ns1", "page1", List.of("page4", "page5"));
        verify(imageRefService).setImageRefsFromPage("site1", "ns1", "page1", List.of("img1", "img2"));
        verify(em).flush();
        verify(regenCacheService).regenCachesForBacklinks("site1", "ns1:page1");

        // Existing Page
        Page p = new Page();
        underTest.updateMetaData("host1", "site1", PageDescriptor.fromFullName("ns1:existing_page"), p, List.of("page4", "page5"), List.of("img1", "img2"));
        verify(linkOverrideService).deleteOverrides("host1", "ns1:existing_page");
        verify(mediaOverrideService).deleteOverrides("host1", "ns1:existing_page");
        verify(linkService).setLinksFromPage("site1", "ns1", "existing_page", List.of("page4", "page5"));
        verify(imageRefService).setImageRefsFromPage("site1", "ns1", "existing_page", List.of("img1", "img2"));
        verify(em, times(1)).flush(); // Only called once: in first call to updateMetaData
        verify(regenCacheService, never()).regenCachesForBacklinks("site1", "ns1:existing_page");

        Page deletedPage = new Page();
        deletedPage.setDeleted(true);
        underTest.updateMetaData("host1", "site1", PageDescriptor.fromFullName("ns1:deleted"), deletedPage, List.of("page4", "page5"), List.of("img1", "img2"));
        verify(linkOverrideService).deleteOverrides("host1", "ns1:deleted");
        verify(mediaOverrideService).deleteOverrides("host1", "ns1:deleted");
        verify(linkService).setLinksFromPage("site1", "ns1", "deleted", List.of("page4", "page5"));
        verify(imageRefService).setImageRefsFromPage("site1", "ns1", "deleted", List.of("img1", "img2"));
        verify(em, times(2)).flush();
        verify(regenCacheService).regenCachesForBacklinks("site1", "ns1:deleted");
    }

    @Test
    void test_deleteMetaData() {
        underTest.deleteMetaData("host1", "site1", PageDescriptor.fromFullName("ns1:page1"));

        PageCache.PageCacheKey key =  new PageCache.PageCacheKey("site1", "ns1", "page1");
        verify(pageCacheRepository).deleteById(key);
        verify(linkService).deleteLinks("site1", "ns1:page1");
        verify(em).flush();
        verify(regenCacheService).regenCachesForBacklinks("site1", "ns1:page1");
        verify(linkOverrideService).deleteOverrides("host1", "ns1:page1");
        verify(mediaOverrideService).deleteOverrides("host1", "ns1:page1");
    }

    @Test
    void test_moveMetaData() {
        when(linkService.getLinksOnPage("site1", "ns1:page1")).thenReturn((List.of("page1")));
        when(imageRefService.getImagesOnPage("site1", "ns1:page1")).thenReturn((List.of("img1")));
        Pair<List<String>, List<String>> linksAndImages = underTest.moveMetaData("host1", "site1", "ns1:page1", "ns2:page2");

        verify(linkOverrideService).createOverride("host1", "ns1:page1", "ns2:page2");
        verify(linkOverrideService).moveOverrides("host1", "ns1:page1", "ns2:page2");
        verify(mediaOverrideService).moveOverrides("host1", "ns1:page1", "ns2:page2");
        assertEquals(List.of("page1"), linksAndImages.getLeft());
        assertEquals(List.of("img1"), linksAndImages.getRight());
    }
}
