package us.calubrecht.lazerwiki.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.LazerWikiApplication;
import us.calubrecht.lazerwiki.model.Link;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {LazerWikiApplication.class})
@ActiveProfiles("test")
public class LinkRepositoryTest {

    @Autowired
    LinkRepository linkRepository;

    @Test
    public void test_findAllBySiteAndSourcePageNSAndSourcePageName() {
        List<Link> fromHome = linkRepository.findAllBySiteAndSourcePageNSAndSourcePageName("default", "", "");
        assertEquals(Set.of("page1", "page2"), fromHome.stream().map(l -> l.getTargetPageName()).collect(Collectors.toSet()));
        List<Link> fromPage1 = linkRepository.findAllBySiteAndSourcePageNSAndSourcePageName("default", "", "page1");
        assertEquals(Set.of(":page2", "ns:nsPage"), fromPage1.stream().map(l -> l.getTargetPageNS() + ":" + l.getTargetPageName()).collect(Collectors.toSet()));
    }

    @Test
    public void test_findAllBySiteAndTargetPageNSAndTargetPageName() {
        List<Link> toPage2 = linkRepository.findAllBySiteAndTargetPageNSAndTargetPageName("default", "", "page2");
        assertEquals(Set.of("", "page1"), toPage2.stream().map(l -> l.getSourcePageName()).collect(Collectors.toSet()));

    }

    @Test
    @Transactional
    public void test_deleteBySiteAndSourcePageNSAndSourcePageName() {
        Link link1 = new Link("default", "", "newPage", "", "pageA");
        Link link2 = new Link("default", "", "newPage", "", "pageB");
        linkRepository.saveAll(List.of(link1, link2));
        List<Link> fromNewPage = linkRepository.findAllBySiteAndSourcePageNSAndSourcePageName("default", "", "newPage");
        assertEquals(Set.of("pageA", "pageB"), fromNewPage.stream().map(l -> l.getTargetPageName()).collect(Collectors.toSet()));

        linkRepository.deleteBySiteAndSourcePageNSAndSourcePageName("default","", "newPage");
        fromNewPage = linkRepository.findAllBySiteAndSourcePageNSAndSourcePageName("default", "", "newPage");
        assertEquals(0, fromNewPage.size());

    }
}
