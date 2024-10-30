package us.calubrecht.lazerwiki.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.model.PageLock;
import us.calubrecht.lazerwiki.responses.PageLockResponse;
import us.calubrecht.lazerwiki.repository.PageLockRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Transactional
public class PageLockService {
    @Autowired
    PageLockRepository repository;

    @Autowired
    PageRepository pageRepository;

    @Autowired
    SiteService siteService;

    @Value("${page.lock.minutes:20}")
    int pageLockMinutes;

    public synchronized PageLockResponse getPageLock(String host, String sPageDescriptor, String userName, boolean overrideLock) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor p = PageService.decodeDescriptor(sPageDescriptor);
        PageLock lock = repository.findBySiteAndNamespaceAndPagename(site, p.namespace(), p.pageName());
        Long revision = pageRepository.getLastRevisionBySiteAndNamespaceAndPagename(site, p.namespace(), p.pageName());
        if (lock == null || lock.getLockTime().isBefore(LocalDateTime.now()) || overrideLock) {
            LocalDateTime lockTime = LocalDateTime.now().plusMinutes(pageLockMinutes);
            lock = new PageLock(site, p.namespace(), p.pageName(), userName, lockTime, newLockId());
            repository.save(lock);
            return new PageLockResponse(p.namespace(), p.pageName(), revision, userName, lockTime, true, lock.getLockId());
        }
        return new PageLockResponse(p.namespace(), p.pageName(), revision, lock.getOwner(), lock.getLockTime(), false, null);
    }

    public synchronized void releasePageLock(String host, String sPageDescriptor, String lockId) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor p = PageService.decodeDescriptor(sPageDescriptor);
        repository.deleteBySiteAndNamespaceAndPagenameAndLockId(site, p.namespace(), p.pageName(), lockId);
    }

    public synchronized void releaseAnyPageLock(String host, String sPageDescriptor) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor p = PageService.decodeDescriptor(sPageDescriptor);
        repository.deleteBySiteAndNamespaceAndPagename(site, p.namespace(), p.pageName());
    }

    String newLockId() {
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(String.format("%08x", r.nextInt()));
        }

        return sb.toString();
    }
}
