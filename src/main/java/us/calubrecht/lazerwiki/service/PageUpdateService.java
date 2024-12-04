package us.calubrecht.lazerwiki.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.*;
import us.calubrecht.lazerwiki.responses.MoveStatus;
import us.calubrecht.lazerwiki.responses.PageLockResponse;
import us.calubrecht.lazerwiki.service.exception.PageRevisionException;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
    LinkOverrideService linkOverrideService;

    @Autowired
    ImageRefService imageRefService;

    @Autowired
    RegenCacheService regenCacheService;

    @Autowired
    PageCacheRepository pageCacheRepository;

    @Autowired
    PageLockService pageLockService;


    @Transactional
    public void savePage(String host, String sPageDescriptor, long lastRevision, String text, Collection<String> tags, Collection<String> links, Collection<String> images, String title, String userName, boolean force) throws PageWriteException{
        String site = siteService.getSiteForHostname(host);
        // get Existing
        PageDescriptor pageDescriptor = PageService.decodeDescriptor(sPageDescriptor);
        if (!namespaceService.canWriteNamespace(site, pageDescriptor.namespace(), userName)) {
            throw new PageWriteException("You don't have permission to write this page.");
        }
        logger.info("Saving Page %s->%s".formatted(site, sPageDescriptor));
        Page p = pageRepository.getBySiteAndNamespaceAndPagename(site, pageDescriptor.namespace(), pageDescriptor.pageName());
        if (p != null && p.getRevision() != lastRevision && !force) {
            throw new PageRevisionException("Expected revision " + p.getRevision() + " but was " + lastRevision);
        }
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
        pageLockService.releaseAnyPageLock(host, sPageDescriptor);
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
        if (!namespaceService.canDeleteInNamespace(site, pageDescriptor.namespace(), userName) || sPageDescriptor.isEmpty()) {
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
        linkOverrideService.deleteOverrides(host, sPageDescriptor);
    }

    @Transactional
    public MoveStatus movePage(String host, String user, String oldPageNS, String oldPageName, String newPageNS, String newPageName) throws PageWriteException {
        String site = siteService.getSiteForHostname(host);

        if (!namespaceService.canWriteNamespace(site, oldPageNS, user)) {
            return new MoveStatus(false, "You don't have permission to write in " + oldPageNS);
        }
        if (!namespaceService.canWriteNamespace(site, newPageNS, user)) {
            return new MoveStatus(false, "You don't have permission to write in " + newPageNS);
        }
        Page existingPage = pageRepository.getBySiteAndNamespaceAndPagename(site, newPageNS, newPageName);
        if (existingPage != null && !existingPage.isDeleted()) {
            return new MoveStatus(false, newPageName + " already exists, move cannot overwrite it");
        }
        String oldPageDescriptor = new PageDescriptor(oldPageNS, oldPageName).toString();
        String newPageDescriptor = new PageDescriptor(newPageNS, newPageName).toString();
        PageLockResponse oldPL = pageLockService.getPageLock(host, oldPageDescriptor, user, false);
        PageLockResponse newPL = pageLockService.getPageLock(host, newPageDescriptor, user, false);
        if (!oldPL.success() || !newPL.success()) {
            pageLockService.releasePageLock(host, oldPageDescriptor, oldPL.pageLockId());
            pageLockService.releasePageLock(host, newPageDescriptor, newPL.pageLockId());
            return new MoveStatus(false, "Could not acquire page locks to move page");
        }

        linkOverrideService.createOverride(host, oldPageDescriptor, newPageDescriptor);
        linkOverrideService.moveOverrides(host, oldPageDescriptor, newPageDescriptor);
        Page oldPage = pageRepository.getBySiteAndNamespaceAndPagename(site, oldPageNS, oldPageName);
        List<String> links = linkService.getLinksOnPage(site, oldPageDescriptor);
        List<String> images = imageRefService.getImagesOnPage(site, oldPageDescriptor);
        savePage(host, new PageDescriptor(newPageNS, newPageName).toString(), 0, oldPage.getText(), oldPage.getTags().stream().map(PageTag::getTag).toList(),
                links, images, oldPage.getTitle(), user, true);
        deletePage(host, oldPageDescriptor, user);
        pageLockService.releasePageLock(host, oldPageDescriptor, oldPL.pageLockId());
        pageLockService.releasePageLock(host, newPageDescriptor, newPL.pageLockId());
        return new MoveStatus(true, oldPageDescriptor + " move to " + newPageDescriptor);
    }


    @Transactional
    public boolean createDefaultSiteHomepage(String siteName, String displayName, String userName) throws PageWriteException, IOException {
        if (pageRepository.getBySiteAndNamespaceAndPagename(siteName, "","") != null) {
            return false;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("defaultSiteHomepage.tmpl")))) {
            String template = br.lines().collect(Collectors.joining(("\n")));
            savePage(siteService.getHostForSitename(siteName), "", 0L, template.replaceAll("%SITENAME%", displayName), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),"Home", userName, false);
        }
        return true;
    }
}
