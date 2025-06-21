package us.calubrecht.lazerwiki.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.model.PageLock;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.responses.PageLockResponse;
import us.calubrecht.lazerwiki.repository.PageLockRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Transactional
public class PageLockService {

    final Logger logger = LogManager.getLogger(getClass());

    @Autowired
    PageLockRepository repository;

    @Autowired
    PageRepository pageRepository;

    @Autowired
    SiteService siteService;

    @Autowired
    UserService userService;

    @Value("${page.lock.minutes:20}")
    int pageLockMinutes;

    public synchronized PageLockResponse getPageLock(String host, String sPageDescriptor, String userName, boolean overrideLock) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor p = PageService.decodeDescriptor(sPageDescriptor);
        PageLock lock = repository.findBySiteAndNamespaceAndPagename(site, p.namespace(), p.pageName());
        Long revision = pageRepository.getLastRevisionBySiteAndNamespaceAndPagename(site, p.namespace(), p.pageName());
        logger.info("Get page lock for " + sPageDescriptor + "-" + userName + "-" + overrideLock + " existingLock=" + lock + " at " + LocalDateTime.now());
        if (lock == null || lock.getLockTime().isBefore(LocalDateTime.now()) || overrideLock) {
            User user = userService.getUser(userName);
            if (lock !=null) {
                p = new PageDescriptor(lock.getNamespace(), lock.getPagename());
            }
            LocalDateTime lockTime = LocalDateTime.now().plusMinutes(pageLockMinutes);
            lock = new PageLock(site, p.namespace(), p.pageName(), user, lockTime, newLockId());
            logger.info("Get get new Lock - " +lock);
            repository.save(lock);
            return new PageLockResponse(p.namespace(), p.pageName(), revision, userName, lockTime, true, lock.getLockId());
        }
        return new PageLockResponse(p.namespace(), p.pageName(), revision, lock.getOwner().userName, lock.getLockTime(), false, null);
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
