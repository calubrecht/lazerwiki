package us.calubrecht.lazerwiki.service;

import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageCache;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.repository.EntityManagerProxy;
import us.calubrecht.lazerwiki.repository.PageCacheRepository;

@Service
public class PageMetaService {
  private final LinkService linkService;

  private final LinkOverrideService linkOverrideService;

  private final MediaOverrideService mediaOverrideService;

  private final ImageRefService imageRefService;

  private final RegenCacheService regenCacheService;

  private final PageCacheRepository pageCacheRepository;

  private final EntityManagerProxy em;

  public PageMetaService(
      @Autowired LinkService linkService,
      @Autowired LinkOverrideService linkOverrideService,
      @Autowired MediaOverrideService mediaOverrideService,
      @Autowired ImageRefService imageRefService,
      @Autowired RegenCacheService regenCacheService,
      @Autowired PageCacheRepository pageCacheRepository,
      @Autowired EntityManagerProxy em) {
    this.linkService = linkService;
    this.linkOverrideService = linkOverrideService;
    this.mediaOverrideService = mediaOverrideService;
    this.imageRefService = imageRefService;
    this.regenCacheService = regenCacheService;
    this.pageCacheRepository = pageCacheRepository;
    this.em = em;
  }

  public void updateMetaData(
      String host,
      String site,
      PageDescriptor pageDescriptor,
      Page p,
      Collection<String> links,
      Collection<String> images) {
    String sPageDescriptor = pageDescriptor.toString();
    linkOverrideService.deleteOverrides(host, sPageDescriptor);
    mediaOverrideService.deleteOverrides(host, sPageDescriptor);
    linkService.setLinksFromPage(
        site, pageDescriptor.namespace(), pageDescriptor.pageName(), links);
    imageRefService.setImageRefsFromPage(
        site, pageDescriptor.namespace(), pageDescriptor.pageName(), images);
    if (p == null || p.isDeleted()) {
      em.flush(); // Flush so regen can work?
      regenCacheService.regenCachesForBacklinks(site, sPageDescriptor);
    }
  }

  public void deleteMetaData(String host, String site, PageDescriptor pageDescriptor) {
    String sPageDescriptor = pageDescriptor.toString();
    PageCache.PageCacheKey key =
        new PageCache.PageCacheKey(site, pageDescriptor.namespace(), pageDescriptor.pageName());
    pageCacheRepository.deleteById(key);
    linkService.deleteLinks(site, sPageDescriptor);
    em.flush(); // Flush so regen can work?
    regenCacheService.regenCachesForBacklinks(site, sPageDescriptor);
    linkOverrideService.deleteOverrides(host, sPageDescriptor);
    mediaOverrideService.deleteOverrides(host, sPageDescriptor);
  }

  public Pair<List<String>, List<String>> moveMetaData(
      String host, String site, String oldPageDescriptor, String newPageDescriptor) {
    linkOverrideService.createOverride(host, oldPageDescriptor, newPageDescriptor);
    linkOverrideService.moveOverrides(host, oldPageDescriptor, newPageDescriptor);
    mediaOverrideService.moveOverrides(host, oldPageDescriptor, newPageDescriptor);
    List<String> links = linkService.getLinksOnPage(site, oldPageDescriptor);
    List<String> images = imageRefService.getImagesOnPage(site, oldPageDescriptor);
    return Pair.of(links, images);
  }
}
