package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.PageCacheRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@SpringBootTest(classes = {RegenCacheService.class})
@ActiveProfiles("test")
class RegenCacheServiceTest {

    @Autowired
    RegenCacheService underTest;

    @MockBean
    PageRepository pageRepository;

    @MockBean
    IMarkupRenderer renderer;

    @MockBean
    LinkService linkService;

    @MockBean
    LinkOverrideService linkOverrideService;

    @MockBean
    ImageRefService imageRefService;

    @MockBean
    PageCacheRepository pageCacheRepository;

    @MockBean
    SiteService siteService;

    @MockBean
    MediaOverrideService mediaOverrideService;


    @Test
    void regenLinks() {
        List<PageDesc> pds = List.of(new PageServiceTest.PageDescImpl("", "page1"), new PageServiceTest.PageDescImpl("ns", "page2"));
        when(pageRepository.getAllValid("default")).thenReturn(pds);
        Page page1 = new Page();
        page1.setPagename("page1");
        page1.setText("text1");
        Page page2 = new Page();
        page2.setPagename("page2");
        page2.setText("text2");

        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(any(), any(), eq("page1"), eq(false))).thenReturn(page1);
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(any(), any(), eq("page2"), eq(false))).thenReturn(page2);
        when(renderer.renderWithInfo(anyString(), any(RenderContext.class))).thenAnswer(inv -> {
            List<String> links = new ArrayList<>();
            String text = inv.getArgument(0, String.class);
            if (text.equals("text1")) {
                links.add("page2");
                links.add("ns:page4");
            }
            else {
                links.add("page3");
                links.add("ns:page4");
            }
            return new RenderResult(text, "", Map.of(RenderResult.RENDER_STATE_KEYS.LINKS.name(), links));
        });

        underTest.regenLinks("default");

        ArgumentCaptor<List<String>> argument = ArgumentCaptor.forClass(List.class);
        verify(linkService,times(2)).setLinksFromPage(eq("default"), any(), any(), argument.capture());
        assertEquals(List.of("page2", "ns:page4"), argument.getAllValues().get(0));
        assertEquals(List.of("page3", "ns:page4"), argument.getAllValues().get(1));
    }

    @Test
    void regenCache() {
        List<PageDesc> pds = List.of(new PageServiceTest.PageDescImpl("", "page1"), new PageServiceTest.PageDescImpl("ns", "page2"));
        when(pageRepository.getAllValid("default")).thenReturn(pds);
        Page page1 = new Page();
        page1.setPagename("page1");
        page1.setText("text1");
        Page page2 = new Page();
        page2.setPagename("page2");
        page2.setText("text2");

        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(any(), any(), eq("page1"), eq(false))).thenReturn(page1);
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(any(), any(), eq("page2"), eq(false))).thenReturn(page2);
        when(renderer.renderWithInfo(anyString(), any(RenderContext.class))).thenAnswer(inv -> {
            List<String> links = new ArrayList<>();
            String text = inv.getArgument(0, String.class);
            return new RenderResult(text + " rendered", "", Map.of(RenderResult.RENDER_STATE_KEYS.LINKS.name(), links));
        });

        underTest.regenCache("default");

        ArgumentCaptor<PageCache> argument = ArgumentCaptor.forClass(PageCache.class);
        verify(pageCacheRepository).deleteBySite("default");
        verify(pageCacheRepository,times(2)).save(argument.capture());
        assertEquals("text1 rendered", argument.getAllValues().get(0).renderedCache);
        assertEquals(true, argument.getAllValues().get(0).useCache);
        assertEquals("text2 rendered", argument.getAllValues().get(1).renderedCache);
    }

    @Test
    void regenCachesForBacklinks() {
        List<PageDesc> pds = List.of(new PageServiceTest.PageDescImpl("", "page1"), new PageServiceTest.PageDescImpl("ns", "page2"));
        when(pageRepository.getAllValid("default")).thenReturn(pds);
        Page page1 = new Page();
        page1.setPagename("page1");
        page1.setText("text1");
        Page page2 = new Page();
        page2.setPagename("page2");
        page2.setText("text2");
        Page page3 = new Page();
        page3.setPagename("page3");
        page3.setText("text3");

        LinkOverride lo = new LinkOverride("default", "", "page3", "", "oldlinkedPage", "", "linkedPage");

        when(linkService.getBacklinks(any(), any())).thenReturn(List.of("page1", "page2"));
        when(linkOverrideService.getOverridesForNewTargetPage("host", "linkedPage")).thenReturn(List.of(lo));
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(any(), any(), eq("page1"), eq(false))).thenReturn(page1);
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(any(), any(), eq("page2"), eq(false))).thenReturn(page2);
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(any(), any(), eq("page3"), eq(false))).thenReturn(page3);
        when(renderer.renderWithInfo(anyString(), any(RenderContext.class))).thenAnswer(inv -> {
            List<String> links = new ArrayList<>();
            String text = inv.getArgument(0, String.class);
            return new RenderResult(text + " rendered", "", Map.of(RenderResult.RENDER_STATE_KEYS.LINKS.name(), links));
        });
        when(siteService.getHostForSitename("default")).thenReturn("host");

        underTest.regenCachesForBacklinks("default", "linkedPage");

        ArgumentCaptor<PageCache> argument = ArgumentCaptor.forClass(PageCache.class);
        verify(pageCacheRepository, never()).deleteBySite("default");
        verify(pageCacheRepository,times(3)).save(argument.capture());
        assertEquals("text1 rendered", argument.getAllValues().get(0).renderedCache);
        assertEquals(true, argument.getAllValues().get(0).useCache);
        assertEquals("text2 rendered", argument.getAllValues().get(1).renderedCache);
        assertEquals("text3 rendered", argument.getAllValues().get(2).renderedCache);

    }

    @Test
    public void testRegenCacheForBacklinksWithNoCache() {
        List<PageDesc> pds = List.of(new PageServiceTest.PageDescImpl("", "page1"), new PageServiceTest.PageDescImpl("ns", "page2"));
        when(pageRepository.getAllValid("default")).thenReturn(pds);
        Page page1 = new Page();
        page1.setPagename("page1");
        page1.setText("text1");

        when(linkService.getBacklinks(any(), any())).thenReturn(List.of("page1"));
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(any(), any(), eq("page1"), eq(false))).thenReturn(page1);
        when(renderer.renderWithInfo(anyString(), any(RenderContext.class))).thenAnswer(inv -> {
            List<String> links = new ArrayList<>();
            String text = inv.getArgument(0, String.class);
            return new RenderResult(text + " rendered", "", Map.of(RenderResult.RENDER_STATE_KEYS.LINKS.name(), links, RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name(), true));
        });

        underTest.regenCachesForBacklinks("default", "linkedPage");

        ArgumentCaptor<PageCache> argument = ArgumentCaptor.forClass(PageCache.class);
        verify(pageCacheRepository, never()).deleteBySite("default");
        verify(pageCacheRepository,times(1)).save(argument.capture());
        assertEquals("text1 rendered", argument.getAllValues().get(0).renderedCache);
        assertEquals(false, argument.getAllValues().get(0).useCache);
    }

    @Test
    public void regenCacheWithNoCache() {
        List<PageDesc> pds = List.of(new PageServiceTest.PageDescImpl("", "page1"));
        when(pageRepository.getAllValid("default")).thenReturn(pds);
        Page page1 = new Page();
        page1.setPagename("page1");
        page1.setText("text1");

        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(any(), any(), eq("page1"), eq(false))).thenReturn(page1);
        when(renderer.renderWithInfo(anyString(), any(RenderContext.class))).thenAnswer(inv -> {
            List<String> links = new ArrayList<>();
            String text = inv.getArgument(0, String.class);
            return new RenderResult(text + " rendered", "", Map.of(RenderResult.RENDER_STATE_KEYS.LINKS.name(), links, RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name(), true));
        });

        underTest.regenCache("default");

        ArgumentCaptor<PageCache> argument = ArgumentCaptor.forClass(PageCache.class);
        verify(pageCacheRepository).deleteBySite("default");
        verify(pageCacheRepository,times(1)).save(argument.capture());
        assertEquals("text1 rendered", argument.getAllValues().get(0).renderedCache);
        assertEquals(false, argument.getAllValues().get(0).useCache);

    }

    @Test
    void regenCachesForImageRefs() {
        List<String> irs = List.of("page2", "ns1:page5");
        when(imageRefService.getRefsForImage("default", "ns1:img1.jpg")).thenReturn(irs);
        List<MediaOverride> overrides = List.of(new MediaOverride("default", "ns1", "page33", "", "img1.jpg", "ns2","img2.jpg"));
        when(mediaOverrideService.getOverridesForImage("host", "ns2:img2.jpg")).thenReturn(overrides);
        Page page2 = new Page();
        page2.setPagename("page2");
        page2.setText("text2");
        Page page5 = new Page();
        page5.setPagename("page5");
        page5.setText("text5");
        Page page33 = new Page();
        page33.setPagename("page33");
        page33.setText("text33");

        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("default", "", "page2", false)).
                thenReturn(page2);
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("default", "ns1", "page5", false)).
                thenReturn(page5);
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("default", "ns1", "page33", false)).
                thenReturn(page33);
        when(renderer.renderWithInfo(anyString(), any(RenderContext.class))).thenAnswer(inv -> {
            List<String> links = new ArrayList<>();
            String text = inv.getArgument(0, String.class);
            Map<String, Object> renderState = new HashMap<>();
            renderState.put(RenderResult.RENDER_STATE_KEYS.LINKS.name(), links);
            if (text.equals("text5")) {
                renderState.put(RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name(), true);
            }
            return new RenderResult(text + " rendered", "", renderState);
        });
        when(siteService.getHostForSitename("default")).thenReturn("host");

        underTest.regenCachesForImageRefs("default", "ns1:img1.jpg", "ns2:img2.jpg");

        ArgumentCaptor<PageCache> argument = ArgumentCaptor.forClass(PageCache.class);
        verify(pageCacheRepository, never()).deleteBySite("default");
        verify(pageCacheRepository,times(3)).save(argument.capture());
        assertEquals("text2 rendered", argument.getAllValues().get(0).renderedCache);
        assertEquals(true, argument.getAllValues().get(0).useCache);
        assertEquals("text5 rendered", argument.getAllValues().get(1).renderedCache);
        assertEquals(false, argument.getAllValues().get(1).useCache);
        assertEquals("text33 rendered", argument.getAllValues().get(2).renderedCache);

    }
}
