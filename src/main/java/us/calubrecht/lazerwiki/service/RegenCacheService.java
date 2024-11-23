package us.calubrecht.lazerwiki.service;

import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.PageCacheRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class RegenCacheService {
    final Logger logger = LogManager.getLogger(getClass());

    @Autowired
    LinkService linkService;

    @Autowired
    ImageRefService imageRefService;

    @Autowired
    IMarkupRenderer renderer;

    @Autowired
    PageRepository pageRepository;

    @Autowired
    PageCacheRepository pageCacheRepository;

    @Autowired
    SiteService siteService;


    @SuppressWarnings("unchecked")
    @Transactional
    public void regenLinks(String site) {
        logger.info("Regening link table for " +site);
        List<PageDesc> pages = pageRepository.getAllValid(site);
        pages.forEach(pd -> {
            Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pd.getNamespace(), pd.getPagename(), false);
            PageDescriptor desc = new PageDescriptor(pd.getNamespace(), pd.getPagename());
            RenderResult res = renderer.renderWithInfo(p.getText(), "", site, desc.toString(), UserService.SYS_USER);
            Collection<String> links = (Collection<String>)res.renderState().getOrDefault(RenderResult.RENDER_STATE_KEYS.LINKS.name(), Collections.emptySet());
            Collection<String> images = (Collection<String>)res.renderState().getOrDefault(RenderResult.RENDER_STATE_KEYS.IMAGES.name(), Collections.emptySet());
            logger.info("Setting " + links.size() + " links for " + pd.getNamespace() + ":" + pd.getPagename());
            linkService.setLinksFromPage(site, pd.getNamespace(), pd.getPagename(), links);
            logger.info("Setting " + links.size() + " images for " + pd.getNamespace() + ":" + pd.getPagename());
            imageRefService.setImageRefsFromPage(site, pd.getNamespace(), pd.getPagename(), images);
        });
    }

    @Transactional
    public void regenCache(String site) {
        String host = siteService.getHostForSitename(site);
        logger.info("Regening cache table for " +site + " " + host);
        pageCacheRepository.deleteBySite(site);
        List<PageDesc> pages = pageRepository.getAllValid(site);
        pages.forEach(pd -> {
            Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pd.getNamespace(), pd.getPagename(), false);
            PageDescriptor desc = new PageDescriptor(pd.getNamespace(), pd.getPagename());
            RenderResult res = renderer.renderWithInfo(p.getText(), host, site, desc.toString(), UserService.SYS_USER);
            PageCache newCache = new PageCache();
            newCache.site = site;
            newCache.namespace = pd.getNamespace();
            newCache.pageName = pd.getPagename();
            newCache.renderedCache = res.renderedText();
            newCache.plaintextCache = res.plainText();
            newCache.title = PageService.getTitle(new PageDescriptor(pd.getNamespace(), pd.getPagename()), p);
            newCache.useCache = !(Boolean)res.renderState().getOrDefault(RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name(), Boolean.FALSE);
            logger.info("Caching rendered page for " + pd.getNamespace() + ":" + pd.getPagename() + " useCache=" + newCache.useCache);
            pageCacheRepository.save(newCache);
        });
    }

    public void regenCachesForBacklinks(String site, String linkedPage) {
        String host = siteService.getHostForSitename(site);
        logger.info("Regening cache for links to " +site + "-" + linkedPage);
        List<String> backlinks = linkService.getBacklinks(site, linkedPage);
        backlinks.forEach(link -> {
            PageDescriptor pd = PageService.decodeDescriptor(link);
            Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pd.namespace(), pd.pageName(), false);
            RenderResult res = renderer.renderWithInfo(p.getText(), host, site, pd.toString(), UserService.SYS_USER);
            PageCache newCache = new PageCache();
            newCache.site = site;
            newCache.namespace = pd.namespace();
            newCache.pageName = pd.pageName();
            newCache.renderedCache = res.renderedText();
            newCache.plaintextCache = res.plainText();
            newCache.title = PageService.getTitle(new PageDescriptor(pd.namespace(), pd.pageName()), p);
            newCache.useCache = !(Boolean)res.renderState().getOrDefault(RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name(), Boolean.FALSE);
            logger.info("Caching rendered page for " + pd.namespace() + ":" + pd.pageName() + " useCache=" + newCache.useCache);
            pageCacheRepository.save(newCache);
        });
    }
}
