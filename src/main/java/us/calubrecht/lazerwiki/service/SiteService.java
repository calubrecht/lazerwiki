package us.calubrecht.lazerwiki.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.util.StreamUtils;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.Site;
import us.calubrecht.lazerwiki.repository.SiteRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class SiteService {

    @Value("${lazerWiki.default.site.title}")
    String defaultTitle;

    @Autowired
    SiteRepository siteRepository;
    @Cacheable("sitesForHostname")
    public String getSiteForHostname(String hostname) {
        Site s =  siteRepository.findByHostname(hostname.toLowerCase());
        if (s != null) {
            return s.name;
        }
        return siteRepository.findByHostname("*").name;
    }

    public String getSiteNameForHostname(String hostname) {
        Site s =  siteRepository.findByHostname(hostname.toLowerCase());
        if (s == null) {
            s = siteRepository.findByHostname("*");
        }
        if (s == null || s.siteName == null) {
            return defaultTitle;
        }
        return s.siteName;
    }

    public Object getSettingForHostname(String hostname, String setting) {
        Site s =  siteRepository.findByHostname(hostname.toLowerCase());
        if (s == null || !s.settings.containsKey(setting)) {
            s = siteRepository.findByHostname("*");
        }
        if (s == null ) {
            return null;
        }
        return s.settings.get(setting);
    }

    public String getHostForSitename(String site) {
        Optional<Site> s =  siteRepository.findById(site);
        return s.map(ss -> ss.hostname).orElse("*");
    }

    public List<String> getAllSites() {
        return StreamSupport.stream(siteRepository.findAll().spliterator(), false).map(site -> site.siteName).toList();
    }
}
