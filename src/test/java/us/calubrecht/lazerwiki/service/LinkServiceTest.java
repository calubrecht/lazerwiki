package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.Link;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.repository.LinkRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;
import us.calubrecht.lazerwiki.service.PageServiceTest.PageDescImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {LinkService.class})
@ActiveProfiles("test")
class LinkServiceTest {
    @Autowired
    LinkService underTest;

    @MockBean
    LinkRepository linkRepository;

    @Test
    void setLinksFromPage() {
        underTest.setLinksFromPage("default", "ns", "pageName", List.of("page1", "ns:page2"));

        verify(linkRepository).deleteBySiteAndSourcePageNSAndSourcePageName("default", "ns", "pageName");
        List<Link> links = List.of(new Link("default", "ns", "pageName", "", "page1"), new Link("default", "ns", "pageName", "ns", "page2"));
        verify(linkRepository).saveAll(eq(links));
    }

    @Test
    void getLinksOnPage() {
        List<Link> links = List.of(new Link("default", "", "pageName", "", "page1"), new Link("default", "", "pageName", "ns:ns:inner", "page5"));
        when(linkRepository.findAllBySiteAndSourcePageNSAndSourcePageName(any(), any(), any())).thenReturn(links);
        assertEquals(List.of("page1", "ns:ns:inner:page5"), underTest.getLinksOnPage("default","pageName"));
    }

    @Test
    void getBacklinks() {
        List<Link> links = List.of(new Link("default", "", "page1", "", "pageName"), new Link("default", "ns:ns:inner", "page5", "ns:ns:inner", "pageName"));
        when(linkRepository.findAllBySiteAndTargetPageNSAndTargetPageName(any(), any(), any())).thenReturn(links);
        assertEquals(List.of("page1", "ns:ns:inner:page5"), underTest.getBacklinks("default","pageName"));
    }
}