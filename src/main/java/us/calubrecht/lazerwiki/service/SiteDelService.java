package us.calubrecht.lazerwiki.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import us.calubrecht.lazerwiki.model.Site;
import us.calubrecht.lazerwiki.repository.*;
import us.calubrecht.lazerwiki.service.exception.MediaWriteException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Service
public class SiteDelService {

    final Logger logger = LogManager.getLogger(getClass());

    @Autowired
    SiteRepository siteRepository;

    @Autowired
    MediaRecordRepository mediaRecordRepository;

    @Autowired
    MediaHistoryRepository mediaHistoryRepository;

    @Autowired
    ImageRefRepository imageRefRepository;

    @Autowired
    LinkRepository linkRepository;

    @Autowired
    NamespaceRepository namespaceRepository;

    @Autowired
    PageRepository pageRepository;

    @Autowired
    PageCacheRepository pageCacheRepository;

    @Autowired
    PageLockRepository pageLockRepository;

    @Value("${lazerwiki.static.file.root}")
    String staticFileRoot;

    @Transactional
    @CacheEvict(value = "sitesForHostname", allEntries = true)
    public boolean deleteSiteCompletely(String siteName, String username) throws MediaWriteException, IOException {
        Site site = siteRepository.findBySiteName(siteName);
        if (site == null) {
            logger.info("Could not delete site siteName=" + siteName + " not found");
            return false;
        }
        String name = site.name;
        logger.warn("Deleting site siteName=" + siteName + " name=" + name + "  by username=" + username);
        mediaRecordRepository.deleteBySite(name);
        mediaHistoryRepository.deleteBySite(name);
        imageRefRepository.deleteBySite(name);
        linkRepository.deleteBySite(name);
        namespaceRepository.deleteBySite(name);
        pageCacheRepository.deleteBySite(name);
        pageLockRepository.deleteBySite(name);
        pageRepository.deleteBySite(name);

        FileSystemUtils.deleteRecursively(new File(String.join("/", staticFileRoot, name)));

        // Last Step
        siteRepository.deleteById(name);
        return true;
    }
}
