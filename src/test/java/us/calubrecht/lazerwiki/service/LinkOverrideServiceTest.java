package us.calubrecht.lazerwiki.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import us.calubrecht.lazerwiki.model.Link;
import us.calubrecht.lazerwiki.model.LinkOverride;
import us.calubrecht.lazerwiki.repository.LinkOverrideRepository;
import us.calubrecht.lazerwiki.repository.LinkRepository;

@SpringBootTest(classes = {LinkOverrideService.class})
@ActiveProfiles("test")
class LinkOverrideServiceTest {
  @Autowired LinkOverrideService underTest;

  @MockitoBean LinkOverrideRepository repo;

  @MockitoBean LinkRepository linkRepo;

  @Test
  void test_getOverrides() {
    List<LinkOverride> retOverrides =
        List.of(
            new LinkOverride("default", "ns", "page", "", "t1", "", "t2"),
            new LinkOverride("default", "ns", "page", "", "t4", "", "t4"));
    when(repo.findAllBySiteAndSourcePageNSAndSourcePageNameOrderById("default", "ns", "page"))
        .thenReturn(retOverrides);

    List<LinkOverride> overrides = underTest.getOverrides("default", "ns:page");
    assertEquals(retOverrides, overrides);
  }

  @Test
  void test_createOverride() {

    LinkOverride lo1 = new LinkOverride("default", "", "p1", "", "pageName", "", "changedPage");
    LinkOverride lo2 = new LinkOverride("default", "ns", "p1", "", "pageName", "", "changedPage");
    Link l1 = new Link("default", "", "p1", "", "pageName");
    Link l2 = new Link("default", "ns", "p1", "", "pageName");
    List<Link> links = List.of(l1, l2);
    when(linkRepo.findAllBySiteAndTargetPageNSAndTargetPageName(any(), eq(""), eq("pageName")))
        .thenReturn(links);

    underTest.createOverride("default", "pageName", "changedPage");
    verify(repo).saveAll(List.of(lo1, lo2));

    LinkOverride lo3 = new LinkOverride("default", "", "p1", "ns", "pageName", "ns", "changedPage");
    Link l3 = new Link("default", "", "p1", "ns", "pageName");

    when(linkRepo.findAllBySiteAndTargetPageNSAndTargetPageName(any(), eq("ns"), eq("pageName")))
        .thenReturn(List.of(l3));
    underTest.createOverride("default", "ns:pageName", "ns:changedPage");

    verify(repo).saveAll(List.of(lo3));
  }

  @Test
  void test_createOverride2ndOverride() {

    LinkOverride lo1 = new LinkOverride("default", "", "p1", "", "pageName", "", "page2");
    LinkOverride lo2 = new LinkOverride("default", "", "p1", "", "pageName", "", "page3");
    Link l1 = new Link("default", "", "p1", "", "pageName");
    List<Link> links = List.of(l1);
    when(linkRepo.findAllBySiteAndTargetPageNSAndTargetPageName(any(), eq(""), eq("pageName")))
        .thenReturn(links);
    when(repo.findAllBySiteAndNewTargetPageNSAndNewTargetPageName(any(), eq(""), eq("page2")))
        .thenReturn(List.of(lo1));

    underTest.createOverride("default", "page2", "page3");
    verify(repo).saveAll(List.of(lo2));
    verify(repo).deleteBySiteAndNewTargetPageNSAndNewTargetPageName("default", "", "page2");
  }

  @Test
  void test_GetOverridesForTargetPage() {
    LinkOverride lo1 = new LinkOverride("default", "", "p1", "", "pageName", "", "page2");
    LinkOverride lo2 = new LinkOverride("default", "", "p1", "", "pageName", "", "page3");
    List<LinkOverride> links = List.of(lo1, lo2);
    when(repo.findAllBySiteAndTargetPageNSAndTargetPageName(any(), eq("ns"), eq("pageName")))
        .thenReturn(links);

    List<LinkOverride> over = underTest.getOverridesForTargetPage("default", "ns:pageName");
    assertEquals(2, over.size());
    assertEquals("page2", over.get(0).getNewTargetPageName());
  }

  @Test
  void test_GetOverridesForNewTargetPage() {
    LinkOverride lo1 = new LinkOverride("default", "", "p1", "", "pageName", "", "page2");
    LinkOverride lo2 = new LinkOverride("default", "", "p1", "", "pageName", "", "page3");
    List<LinkOverride> links = List.of(lo1, lo2);
    when(repo.findAllBySiteAndNewTargetPageNSAndNewTargetPageName(any(), eq("ns"), eq("pageName")))
        .thenReturn(links);

    List<LinkOverride> over = underTest.getOverridesForNewTargetPage("default", "ns:pageName");
    assertEquals(2, over.size());
    assertEquals("page2", over.get(0).getNewTargetPageName());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void test_moveOverrides() {
    LinkOverride lo1 = new LinkOverride("default", "", "p1", "", "pageName", "", "page2");
    LinkOverride lo2 = new LinkOverride("default", "", "p1", "", "pageName", "", "page3");
    List<LinkOverride> links = List.of(lo1, lo2);
    when(repo.findAllBySiteAndSourcePageNSAndSourcePageNameOrderById(any(), eq("ns1"), eq("page")))
        .thenReturn(links);
    underTest.moveOverrides("default", "ns1:page", "ns2:page");

    verify(repo).deleteBySiteAndNewTargetPageNSAndNewTargetPageName(any(), eq("ns1"), eq("page"));
    ArgumentCaptor<List<LinkOverride>> listCaptor = ArgumentCaptor.forClass(List.class);
    verify(repo).saveAll(listCaptor.capture());

    assertEquals(2, listCaptor.getValue().size());
    LinkOverride over = (LinkOverride) listCaptor.getValue().get(0);
    assertEquals("page", over.getSourcePageName());
    assertEquals("ns2", over.getSourcePageNS());
  }

  @Test
  public void test_deleteOverrides() {
    underTest.deleteOverrides("default", "ns1:page");

    verify(repo).deleteBySiteAndSourcePageNSAndSourcePageName("default", "ns1", "page");
  }
}
