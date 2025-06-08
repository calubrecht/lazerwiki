package us.calubrecht.lazerwiki.service;

import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.PageCacheRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.*;

@Service
public class RegenCacheService {
    final Logger logger = LogManager.getLogger(getClass());

    @Autowired
    LinkService linkService;

    @Autowired
    LinkOverrideService linkOverrideService;

    @Autowired
    MediaOverrideService mediaOverrideService;

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
        String siteKey = site.toLowerCase();
        logger.info("Regening link table for " +siteKey);
        List<PageDesc> pages = pageRepository.getAllValid(siteKey);
        pages.forEach(pd -> {
            Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(siteKey, pd.getNamespace(), pd.getPagename(), false);
            PageDescriptor desc = new PageDescriptor(pd.getNamespace(), pd.getPagename());
            RenderContext renderContext = new RenderContext("", siteKey, desc.toString(), UserService.SYS_USER);
            renderContext.renderState().put(RenderResult.RENDER_STATE_KEYS.FOR_CACHE.name(), Boolean.TRUE);
            RenderResult res = renderer.renderWithInfo(p.getText(), renderContext);
            Collection<String> links = (Collection<String>)res.renderState().getOrDefault(RenderResult.RENDER_STATE_KEYS.LINKS.name(), Collections.emptySet());
            Collection<String> images = (Collection<String>)res.renderState().getOrDefault(RenderResult.RENDER_STATE_KEYS.IMAGES.name(), Collections.emptySet());
            logger.info("Setting " + links.size() + " links for " + pd.getNamespace() + ":" + pd.getPagename());
            linkService.setLinksFromPage(siteKey, pd.getNamespace(), pd.getPagename(), links);
            logger.info("Setting " + links.size() + " images for " + pd.getNamespace() + ":" + pd.getPagename());
            imageRefService.setImageRefsFromPage(siteKey, pd.getNamespace(), pd.getPagename(), images);
        });
    }

    @Transactional
    public void regenCache(String site) {
        String siteKey = site.toLowerCase();
        String host = siteService.getHostForSitename(siteKey);
        logger.info("Regening cache table for " +siteKey + " " + host);
        pageCacheRepository.deleteBySite(siteKey);
        List<PageDesc> pages = pageRepository.getAllValid(siteKey);
        pages.forEach(pd -> {
            Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(siteKey, pd.getNamespace(), pd.getPagename(), false);
            PageDescriptor desc = new PageDescriptor(pd.getNamespace(), pd.getPagename());
            RenderContext renderContext = new RenderContext(host, siteKey, desc.toString(), UserService.SYS_USER);
            renderContext.renderState().put(RenderResult.RENDER_STATE_KEYS.FOR_CACHE.name(), Boolean.TRUE);
            logger.info("Render: " + desc.toString());
            RenderResult res = renderer.renderWithInfo(p.getText(), renderContext);
            PageCache newCache = new PageCache();
            newCache.site = siteKey;
            newCache.namespace = pd.getNamespace();
            newCache.pageName = pd.getPagename();
            newCache.renderedCache = res.renderedText();
            newCache.plaintextCache = res.plainText();
            newCache.title = PageService.getTitle(new PageDescriptor(pd.getNamespace(), pd.getPagename()), p);
            newCache.source = PageService.doAdjustSource(p.getText(), res);
            newCache.useCache = !(Boolean)res.renderState().getOrDefault(RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name(), Boolean.FALSE);
            logger.info("Caching rendered page for " + pd.getNamespace() + ":" + pd.getPagename() + " useCache=" + newCache.useCache);
            pageCacheRepository.save(newCache);
        });
    }

    public void regenCachesForBacklinks(String site, String linkedPage) {
        String host = siteService.getHostForSitename(site);
        logger.info("Regening cache for links to " +site + "-" + linkedPage);
        List<String> backlinks = linkService.getBacklinks(site, linkedPage);
        List<String> overrideBacklinks = linkOverrideService.getOverridesForNewTargetPage(host, linkedPage).stream().map(
                LinkOverride::getSource
        ).toList();
        List<String> allLinks = new ArrayList<>();
        allLinks.addAll(backlinks);
        allLinks.addAll(overrideBacklinks);
        allLinks.stream().distinct().forEach(link -> {
            PageDescriptor pd = PageService.decodeDescriptor(link);
            Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pd.namespace(), pd.pageName(), false);
            RenderContext renderContext = new RenderContext(host, site, pd.toString(), UserService.SYS_USER);
            renderContext.renderState().put(RenderResult.RENDER_STATE_KEYS.FOR_CACHE.name(), Boolean.TRUE);
            RenderResult res = renderer.renderWithInfo(p.getText(), renderContext);
            PageCache newCache = new PageCache();
            newCache.site = site;
            newCache.namespace = pd.namespace();
            newCache.pageName = pd.pageName();
            newCache.renderedCache = res.renderedText();
            newCache.plaintextCache = res.plainText();
            newCache.title = PageService.getTitle(new PageDescriptor(pd.namespace(), pd.pageName()), p);
            newCache.source = PageService.doAdjustSource(p.getText(), res);
            newCache.useCache = !(Boolean)res.renderState().getOrDefault(RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name(), Boolean.FALSE);
            logger.info("Caching rendered page for " + pd.namespace() + ":" + pd.pageName() + " useCache=" + newCache.useCache);
            pageCacheRepository.save(newCache);
        });
    }

    public void regenCachesForImageRefs(String site, String oldImageRef, String newImageRef) {
        String host = siteService.getHostForSitename(site);
        logger.info("Regening cache for media links to " +site + "-" + oldImageRef);
        List<String> backlinks = imageRefService.getRefsForImage(site, oldImageRef);
        List<String> overrideBacklinks = mediaOverrideService.getOverridesForImage(host, newImageRef).stream().map(
                MediaOverride::getSource
        ).toList();
        List<String> allLinks = new ArrayList<>();
        allLinks.addAll(backlinks);
        allLinks.addAll(overrideBacklinks);
        allLinks.stream().distinct().forEach(link -> {
            PageDescriptor pd = PageService.decodeDescriptor(link);
            Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pd.namespace(), pd.pageName(), false);
            RenderContext renderContext = new RenderContext(host, site, pd.toString(), UserService.SYS_USER);
            renderContext.renderState().put(RenderResult.RENDER_STATE_KEYS.FOR_CACHE.name(), Boolean.TRUE);
            RenderResult res = renderer.renderWithInfo(p.getText(), renderContext);
            PageCache newCache = new PageCache();
            newCache.site = site;
            newCache.namespace = pd.namespace();
            newCache.pageName = pd.pageName();
            newCache.renderedCache = res.renderedText();
            newCache.plaintextCache = res.plainText();
            newCache.title = PageService.getTitle(new PageDescriptor(pd.namespace(), pd.pageName()), p);
            newCache.source = PageService.doAdjustSource(p.getText(), res);
            newCache.useCache = !(Boolean)res.renderState().getOrDefault(RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name(), Boolean.FALSE);
            logger.info("Caching rendered page for " + pd.namespace() + ":" + pd.pageName() + " useCache=" + newCache.useCache);
            pageCacheRepository.save(newCache);
        });
    }
}
