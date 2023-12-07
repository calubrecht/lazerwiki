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
public class AdminService {
    Logger logger = LogManager.getLogger(getClass());

    @Autowired
    LinkService linkService;

    @Autowired
    IMarkupRenderer renderer;

    @Autowired
    PageRepository pageRepository;

    @Autowired
    PageCacheRepository pageCacheRepository;

    @Transactional
    public void regenLinks(String site) {
        logger.info("Regening link table for " +site);
        List<PageDesc> pages = pageRepository.getAllValid(site);
        pages.forEach(pd -> {
            Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pd.getNamespace(), pd.getPagename(), false);
            RenderResult res = renderer.renderWithInfo(p.getText(), "", site, UserService.SYS_USER);
            Collection<String> links = (Collection<String>)res.renderState().getOrDefault(RenderResult.RENDER_STATE_KEYS.LINKS.name(), Collections.emptySet());
            logger.info("Setting " + links.size() + " links for " + pd.getNamespace() + ":" + pd.getPagename());
            linkService.setLinksFromPage(site, pd.getNamespace(), pd.getPagename(), links);
        });
    }

    @Transactional
    public void regenCache(String site) {
        logger.info("Regening cache table for " +site);
        pageCacheRepository.deleteBySite(site);
        List<PageDesc> pages = pageRepository.getAllValid(site);
        pages.forEach(pd -> {
            Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pd.getNamespace(), pd.getPagename(), false);
            RenderResult res = renderer.renderWithInfo(p.getText(), "", site, UserService.SYS_USER);
            PageCache newCache = new PageCache();
            newCache.site = site;
            newCache.namespace = pd.getNamespace();
            newCache.pageName = pd.getPagename();
            newCache.renderedCache = res.renderedText();
            newCache.plaintextCache = res.plainText();
            newCache.useCache = !(Boolean)res.renderState().getOrDefault(RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name(), Boolean.FALSE);
            logger.info("Caching rendered page for " + pd.getNamespace() + ":" + pd.getPagename() + " useCache=" + newCache.useCache);
            pageCacheRepository.save(newCache);
        });
    }
}
