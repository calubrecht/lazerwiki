package us.calubrecht.lazerwiki.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageCache;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.model.PageTag;
import us.calubrecht.lazerwiki.repository.*;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
public class PageUpdateService {
    final Logger logger = LogManager.getLogger(getClass());

    @Autowired
    PageRepository pageRepository;

    @Autowired
    IdRepository idRepository;

    @Autowired
    SiteService siteService;

    @Autowired
    EntityManagerProxy em;

    @Autowired
    NamespaceService namespaceService;

    @Autowired
    LinkService linkService;

    @Autowired
    ImageRefService imageRefService;

    @Autowired
    RegenCacheService regenCacheService;

    @Autowired
    PageCacheRepository pageCacheRepository;

    @Autowired
    PageLockService pageLockService;

    @Transactional
    public void savePage(String host, String sPageDescriptor, String text, Collection<String> tags, Collection<String> links, Collection<String> images, String title, String userName) throws PageWriteException{
        String site = siteService.getSiteForHostname(host);
        // get Existing
        PageDescriptor pageDescriptor = PageService.decodeDescriptor(sPageDescriptor);
        if (!namespaceService.canWriteNamespace(site, pageDescriptor.namespace(), userName)) {
            throw new PageWriteException("You don't have permission to write this page.");
        }
        logger.info("Saving Page %s->%s".formatted(site, sPageDescriptor));
        Page p = pageRepository.getBySiteAndNamespaceAndPagename(site, pageDescriptor.namespace(), pageDescriptor.pageName());
        long id = p == null ? getNewId() : p.getId();
        long revision = p == null ? 1 : p.getRevision() + 1;
        if (p != null ) {
            p.setValidts(LocalDateTime.now());
            pageRepository.save(p);
            em.flush(); // Flush update to DB so we can do insert afterward
        }
        Page newP = new Page();
        newP.setSite(site);
        newP.setNamespace(pageDescriptor.namespace());
        newP.setPagename(pageDescriptor.pageName());
        newP.setText(text);
        newP.setTitle(title);
        newP.setId(id);
        newP.setRevision(revision);
        newP.setValidts(PageRepository.MAX_DATE);
        newP.setModifiedBy(userName);
        newP.setTags(tags.stream().map(s -> new PageTag(newP, s)).toList());
        pageRepository.save(newP);
        pageLockService.releasePageLock(host, sPageDescriptor, null);
        linkService.setLinksFromPage(site, pageDescriptor.namespace(), pageDescriptor.pageName(), links);
        imageRefService.setImageRefsFromPage(site, pageDescriptor.namespace(), pageDescriptor.pageName(), images);
        if (p == null  || p.isDeleted()) {
            em.flush(); // Flush so regen can work?
            regenCacheService.regenCachesForBacklinks(site,sPageDescriptor);
        }
    }

    protected long getNewId() {

        return idRepository.getNewId();
    }

    @Transactional
    public void deletePage(String host, String sPageDescriptor, String userName) throws PageWriteException {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = PageService.decodeDescriptor(sPageDescriptor);
        if (!namespaceService.canDeleteInNamespace(site, pageDescriptor.namespace(), userName) || sPageDescriptor.equals("")) {
            throw new PageWriteException("You don't have permission to delete this page.");
        }
        logger.info("Deleting Page %s->%s".formatted(site, sPageDescriptor));

        Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pageDescriptor.namespace(), pageDescriptor.pageName(), false);
        if (p == null) {
            return;
        }
        long revision =  p.getRevision() + 1;
        p.setValidts(LocalDateTime.now());
        pageRepository.save(p);
        em.flush(); // Flush update to DB so we can do insert afterward
        Page newP = new Page();
        newP.setSite(site);
        newP.setNamespace(pageDescriptor.namespace());
        newP.setPagename(pageDescriptor.pageName());
        newP.setText("");
        newP.setTitle("");
        newP.setId(p.getId());
        newP.setRevision(revision);
        newP.setValidts(PageRepository.MAX_DATE);
        newP.setModifiedBy(userName);
        newP.setTags(Collections.emptyList());
        newP.setDeleted(true);
        pageRepository.save(newP);
        linkService.deleteLinks(site, sPageDescriptor);
        PageCache.PageCacheKey key = new PageCache.PageCacheKey(site, pageDescriptor.namespace(), pageDescriptor.pageName());
        pageCacheRepository.deleteById(key);
        em.flush(); // Flush so regen can work?
        regenCacheService.regenCachesForBacklinks(site,sPageDescriptor);
    }


    @Transactional
    public boolean createDefaultSiteHomepage(String siteName, String displayName, String userName) throws PageWriteException, IOException {
        if (pageRepository.getBySiteAndNamespaceAndPagename(siteName, "","") != null) {
            return false;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("defaultSiteHomepage.tmpl")))) {
            String template = br.lines().collect(Collectors.joining(("\n")));
            savePage(siteService.getHostForSitename(siteName), "", template.replaceAll("%SITENAME%", displayName), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),"Home", userName);
        }
        return true;
    }
}
