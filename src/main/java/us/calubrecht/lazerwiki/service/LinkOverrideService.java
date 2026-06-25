package us.calubrecht.lazerwiki.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.Link;
import us.calubrecht.lazerwiki.model.LinkOverride;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.repository.LinkOverrideRepository;
import us.calubrecht.lazerwiki.repository.LinkRepository;

@Service
public class LinkOverrideService {
  @Autowired LinkOverrideRepository repo;

  @Autowired LinkRepository linkRepo;

  public List<LinkOverride> getOverrides(String site, String pageName) {
    PageDescriptor pageDescriptor = PageService.decodeDescriptor(pageName);
    return repo.findAllBySiteAndSourcePageNSAndSourcePageNameOrderById(
        site, pageDescriptor.namespace(), pageDescriptor.pageName());
  }

  public List<LinkOverride> getOverridesForTargetPage(String site, String pageName) {
    PageDescriptor pageDescriptor = PageService.decodeDescriptor(pageName);
    return repo.findAllBySiteAndTargetPageNSAndTargetPageName(
        site, pageDescriptor.namespace(), pageDescriptor.pageName());
  }

  public List<LinkOverride> getOverridesForNewTargetPage(String site, String pageName) {
    PageDescriptor pageDescriptor = PageService.decodeDescriptor(pageName);
    return repo.findAllBySiteAndNewTargetPageNSAndNewTargetPageName(
        site, pageDescriptor.namespace(), pageDescriptor.pageName());
  }

  @Transactional
  public void createOverride(String site, String pageName, String changedPage) {
    PageDescriptor pageDescriptor = PageService.decodeDescriptor(pageName);
    PageDescriptor newPageDescriptor = PageService.decodeDescriptor(changedPage);
    List<LinkOverride> existingLinkOverride =
        repo.findAllBySiteAndNewTargetPageNSAndNewTargetPageName(
            site, pageDescriptor.namespace(), pageDescriptor.pageName());
    repo.deleteBySiteAndNewTargetPageNSAndNewTargetPageName(
        site, pageDescriptor.namespace(), pageDescriptor.pageName());
    List<Link> linksToOverride =
        linkRepo.findAllBySiteAndTargetPageNSAndTargetPageName(
            site, pageDescriptor.namespace(), pageDescriptor.pageName());
    List<LinkOverride> linkOverrides =
        linksToOverride.stream()
            .map(
                link ->
                    new LinkOverride(
                        site,
                        link.getSourcePageNS(),
                        link.getSourcePageName(),
                        pageDescriptor.namespace(),
                        pageDescriptor.pageName(),
                        newPageDescriptor.namespace(),
                        newPageDescriptor.pageName()))
            .toList();
    repo.saveAll(linkOverrides);
    List<LinkOverride> replacedLinkOverrides =
        existingLinkOverride.stream()
            .map(
                lo ->
                    new LinkOverride(
                        lo.getSite(),
                        lo.getSourcePageNS(),
                        lo.getSourcePageName(),
                        lo.getTargetPageNS(),
                        lo.getTargetPageName(),
                        newPageDescriptor.namespace(),
                        newPageDescriptor.pageName()))
            .toList();
    repo.saveAll(replacedLinkOverrides);
  }

  @Transactional
  public void moveOverrides(String site, String oldPage, String newPage) {
    PageDescriptor pageDescriptor = PageService.decodeDescriptor(oldPage);
    PageDescriptor newPageDescriptor = PageService.decodeDescriptor(newPage);
    List<LinkOverride> linkOverridesToCopy =
        repo.findAllBySiteAndSourcePageNSAndSourcePageNameOrderById(
            site, pageDescriptor.namespace(), pageDescriptor.pageName());
    List<LinkOverride> replacedLinkOverrides =
        linkOverridesToCopy.stream()
            .map(
                lo ->
                    new LinkOverride(
                        lo.getSite(),
                        newPageDescriptor.namespace(),
                        newPageDescriptor.pageName(),
                        lo.getTargetPageNS(),
                        lo.getTargetPageName(),
                        lo.getNewTargetPageNS(),
                        lo.getNewTargetPageName()))
            .toList();
    repo.deleteBySiteAndNewTargetPageNSAndNewTargetPageName(
        site, pageDescriptor.namespace(), pageDescriptor.pageName());
    repo.saveAll(replacedLinkOverrides);
  }

  @Transactional
  public void deleteOverrides(String site, String page) {
    PageDescriptor pageDescriptor = PageService.decodeDescriptor(page);
    repo.deleteBySiteAndSourcePageNSAndSourcePageName(
        site, pageDescriptor.namespace(), pageDescriptor.pageName());
  }
}
