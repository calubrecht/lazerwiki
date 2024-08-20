package us.calubrecht.lazerwiki.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.model.PageLock;
import us.calubrecht.lazerwiki.responses.PageLockResponse;
import us.calubrecht.lazerwiki.repository.PageLockRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;

import java.time.LocalDateTime;

@Service
public class PageLockService {
    @Autowired
    PageLockRepository repository;

    @Autowired
    PageRepository pageRepository;

    @Autowired
    SiteService siteService;

    public synchronized PageLockResponse getPageLock(String host, String sPageDescriptor, String userName, boolean overrideLock) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor p = PageService.decodeDescriptor(sPageDescriptor);
        PageLock lock = repository.findBySiteAndNamespaceAndPagename(site, p.namespace(), p.pageName());
        Long revision = pageRepository.getLastRevisionBySiteAndNamespaceAndPagename(site, p.namespace(), p.pageName());
        if (lock == null || lock.getLockTime().isBefore(LocalDateTime.now()) || overrideLock) {
            LocalDateTime lockTime = LocalDateTime.now().plusMinutes(5);
            lock = new PageLock(site, p.namespace(), p.pageName(), userName, lockTime);
            repository.save(lock);
            return new PageLockResponse(p.namespace(), p.pageName(), revision, userName, lockTime, true);
        }
        return new PageLockResponse(p.namespace(), p.pageName(), revision, lock.getOwner(), lock.getLockTime(), false);
    }

    // TODO: Still need releasePageLock
}
