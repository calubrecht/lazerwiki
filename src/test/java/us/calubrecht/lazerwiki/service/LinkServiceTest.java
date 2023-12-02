package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.Link;
import us.calubrecht.lazerwiki.repository.LinkRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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
}