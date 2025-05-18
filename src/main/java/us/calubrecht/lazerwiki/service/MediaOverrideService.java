package us.calubrecht.lazerwiki.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.ImageRef;
import us.calubrecht.lazerwiki.model.MediaOverride;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.repository.ImageRefRepository;
import us.calubrecht.lazerwiki.repository.MediaOverrideRepository;

import java.util.List;

@Service
public class MediaOverrideService {
    @Autowired
    SiteService siteService;

    @Autowired
    MediaOverrideRepository repo;

    @Autowired
    ImageRefRepository imageRefRepository;

    public List<MediaOverride> getOverrides(String host, String pageName) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = PageService.decodeDescriptor(pageName);
       return repo.findAllBySiteAndSourcePageNSAndSourcePageNameOrderById(site, pageDescriptor.namespace(), pageDescriptor.pageName());
    }

    public List<MediaOverride> getOverridesForImage(String host, String imageName) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = PageService.decodeDescriptor(imageName);
        return repo.findAllBySiteAndNewTargetFileNSAndNewTargetFileName(site, pageDescriptor.namespace(), pageDescriptor.pageName());
    }

    @Transactional
    public void createOverride(String host, String oldFileNS, String oldFileName, String newFileNS, String newFileName) {
        String site = siteService.getSiteForHostname(host);
        List<MediaOverride> existingLinkOverride = repo.findAllBySiteAndNewTargetFileNSAndNewTargetFileName(site, oldFileNS, oldFileName);
        repo.deleteBySiteAndNewTargetFileNSAndNewTargetFileName(site, oldFileNS, oldFileName);
        List<ImageRef> linksToOverride = imageRefRepository.findAllBySiteAndImageNSAndImageRef(site, oldFileNS, oldFileName);
        List<MediaOverride> linkOverrides = linksToOverride.stream().map(link ->
                new MediaOverride(site, link.getSourcePageNS(), link.getSourcePageName(), oldFileNS, oldFileName, newFileNS, newFileName)).toList();
        repo.saveAll(linkOverrides);
        List<MediaOverride> replacedLinkOverrides = existingLinkOverride.stream().map(lo -> new MediaOverride(lo.getSite(), lo.getSourcePageNS(), lo.getSourcePageName(), lo.getTargetFileNS(), lo.getTargetFileName(), newFileNS, newFileName)).toList();
        repo.saveAll(replacedLinkOverrides);
    }

    @Transactional
    public void moveOverrides(String host, String oldPage, String newPage) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = PageService.decodeDescriptor(oldPage);
        PageDescriptor newPageDescriptor = PageService.decodeDescriptor(newPage);
        List<MediaOverride> linkOverridesToCopy = repo.findAllBySiteAndSourcePageNSAndSourcePageNameOrderById(site, pageDescriptor.namespace(), pageDescriptor.pageName());
        List<MediaOverride> replacedLinkOverrides = linkOverridesToCopy.stream().map(lo -> new MediaOverride(lo.getSite(), newPageDescriptor.namespace(), newPageDescriptor.pageName(), lo.getTargetFileNS(), lo.getTargetFileName(), lo.getNewTargetFileNS(), lo.getNewTargetFileName())).toList();
        repo.deleteBySiteAndSourcePageNSAndSourcePageName(site, pageDescriptor.namespace(), pageDescriptor.pageName());
        repo.saveAll(replacedLinkOverrides);
    }

    @Transactional
    public void deleteOverrides(String host, String page) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = PageService.decodeDescriptor(page);
        repo.deleteBySiteAndSourcePageNSAndSourcePageName(site, pageDescriptor.namespace(), pageDescriptor.pageName());
    }
}
