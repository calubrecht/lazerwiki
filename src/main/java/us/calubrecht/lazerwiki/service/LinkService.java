package us.calubrecht.lazerwiki.service;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.LinkRepository;

@Service
public class LinkService {
  @Autowired LinkRepository linkRepository;

  @Transactional
  public void setLinksFromPage(
      String site, String pageNS, String pageName, Collection<String> targets) {
    linkRepository.deleteBySiteAndSourcePageNSAndSourcePageName(site, pageNS, pageName);
    List<Link> links =
        targets.stream()
            .map(
                t -> {
                  PageDescriptor pd = PageService.decodeDescriptor(t);
                  return new Link(site, pageNS, pageName, pd.namespace(), pd.pageName());
                })
            .collect(Collectors.toList());
    linkRepository.saveAll(links);
  }

  @Transactional(readOnly = true)
  public List<String> getLinksOnPage(String site, String page) {
    PageDescriptor pd = PageService.decodeDescriptor(page);
    return linkRepository
        .findAllBySiteAndSourcePageNSAndSourcePageName(site, pd.namespace(), pd.pageName())
        .stream()
        .map(
            l ->
                l.getTargetPageNS().isBlank()
                    ? l.getTargetPageName()
                    : l.getTargetPageNS() + ":" + l.getTargetPageName())
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<String> getBacklinks(String site, String page) {
    PageDescriptor pd = PageService.decodeDescriptor(page);
    return linkRepository
        .findAllBySiteAndTargetPageNSAndTargetPageName(site, pd.namespace(), pd.pageName())
        .stream()
        .map(
            l ->
                l.getSourcePageNS().isBlank()
                    ? l.getSourcePageName()
                    : l.getSourcePageNS() + ":" + l.getSourcePageName())
        .collect(Collectors.toList());
  }

  public void deleteLinks(String site, String page) {
    PageDescriptor pd = PageService.decodeDescriptor(page);
    linkRepository.deleteBySiteAndSourcePageNSAndSourcePageName(
        site, pd.namespace(), pd.pageName());
  }
}
