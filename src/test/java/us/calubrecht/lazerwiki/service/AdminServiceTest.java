package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageCache;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.repository.PageCacheRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@SpringBootTest(classes = {AdminService.class})
@ActiveProfiles("test")
class AdminServiceTest {

    @Autowired
    AdminService underTest;

    @MockBean
    PageRepository pageRepository;

    @MockBean
    IMarkupRenderer renderer;

    @MockBean
    LinkService linkService;

    @MockBean
    PageCacheRepository pageCacheRepository;


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
        when(renderer.renderWithInfo(any(), any(), any(), any())).thenAnswer(inv -> {
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
        when(renderer.renderWithInfo(any(), any(), any(), any())).thenAnswer(inv -> {
            List<String> links = new ArrayList<>();
            String text = inv.getArgument(0, String.class);
            return new RenderResult(text + " rendered", "", Map.of(RenderResult.RENDER_STATE_KEYS.LINKS.name(), links));
        });

        underTest.regenCache("default");

        ArgumentCaptor<PageCache> argument = ArgumentCaptor.forClass(PageCache.class);
        verify(pageCacheRepository).deleteBySite("default");
        verify(pageCacheRepository,times(2)).save(argument.capture());
        assertEquals("text1 rendered", argument.getAllValues().get(0).renderedCache);
        assertEquals("text2 rendered", argument.getAllValues().get(1).renderedCache);
    }
}
