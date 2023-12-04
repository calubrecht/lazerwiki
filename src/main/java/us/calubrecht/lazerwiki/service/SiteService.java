package us.calubrecht.lazerwiki.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.Site;
import us.calubrecht.lazerwiki.repository.SiteRepository;

@Service
public class SiteService {

    @Value("${lazerWiki.default.site.title}")
    String defaultTitle;

    @Autowired
    SiteRepository siteRepository;
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
}
