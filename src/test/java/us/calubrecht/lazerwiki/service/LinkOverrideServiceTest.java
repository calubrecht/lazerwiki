package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.Link;
import us.calubrecht.lazerwiki.model.LinkOverride;
import us.calubrecht.lazerwiki.repository.LinkOverrideRepository;
import us.calubrecht.lazerwiki.repository.LinkRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SpringBootTest(classes = {LinkOverrideService.class})
@ActiveProfiles("test")
class LinkOverrideServiceTest {
    @Autowired
    LinkOverrideService underTest;

    @MockBean
    LinkOverrideRepository repo;

    @MockBean
    LinkRepository linkRepo;

    @MockBean
    SiteService siteService;

    @Test
    void test_getOverrides() {
        List<LinkOverride> retOverrides = List.of(
                new LinkOverride("default", "ns", "page", "", "t1", "", "t2"),
                new LinkOverride("default", "ns", "page", "", "t4", "", "t4")
        );
        when(repo.findAllBySiteAndSourcePageNSAndSourcePageNameOrderById("default", "ns", "page")).thenReturn(retOverrides);
        when(siteService.getSiteForHostname(eq("host"))).thenReturn("default");

        List<LinkOverride> overrides = underTest.getOverrides("host", "ns:page");
        assertEquals(retOverrides, overrides);
    }

    @Test
    void testCreateOverride() {

        LinkOverride lo1 = new LinkOverride("default", "", "p1", "", "pageName", "", "changedPage");
        LinkOverride lo2 = new LinkOverride("default", "ns", "p1", "", "pageName", "", "changedPage");
        Link l1 = new Link("default", "", "p1", "", "pageName");
        Link l2 = new Link("default", "ns", "p1", "", "pageName");
        List<Link> links = List.of(l1, l2);
        when(siteService.getSiteForHostname(eq("host"))).thenReturn("default");
        when(linkRepo.findAllBySiteAndTargetPageNSAndTargetPageName(any(), eq(""), eq("pageName"))).
                thenReturn(links);

        underTest.createOverride("host", "pageName", "changedPage");
        verify(repo).saveAll(List.of(lo1, lo2));

        LinkOverride lo3 = new LinkOverride("default", "", "p1", "ns", "pageName", "ns", "changedPage");
        Link l3 = new Link("default", "", "p1", "ns", "pageName");

        when(linkRepo.findAllBySiteAndTargetPageNSAndTargetPageName(any(), eq("ns"), eq("pageName"))).
                thenReturn(List.of(l3));
        underTest.createOverride("host", "ns:pageName", "ns:changedPage");

        verify(repo).saveAll(List.of(lo3));
    }

    @Test
    void testCreateOverride2ndOverride() {

        LinkOverride lo1 = new LinkOverride("default", "", "p1", "", "pageName", "", "page2");
        LinkOverride lo2 = new LinkOverride("default", "", "p1", "", "pageName", "", "page3");
        Link l1 = new Link("default", "", "p1", "", "pageName");
        List<Link> links = List.of(l1);
        when(siteService.getSiteForHostname(eq("host"))).thenReturn("default");
        when(linkRepo.findAllBySiteAndTargetPageNSAndTargetPageName(any(), eq(""), eq("pageName"))).
                thenReturn(links);
        when(repo.findAllBySiteAndNewTargetPageNSAndNewTargetPageName(any(), eq(""), eq("page2"))).
                thenReturn(List.of(lo1));

        underTest.createOverride("host", "page2", "page3");
        verify(repo).saveAll(List.of(lo2));
        verify(repo).deleteBySiteAndNewTargetPageNSAndNewTargetPageName("default", "", "page2");
    }
}