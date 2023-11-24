package us.calubrecht.lazerwiki.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.Site;
import us.calubrecht.lazerwiki.repository.SiteRepository;

@Service
public class SiteService {

    @Autowired
    SiteRepository siteRepository;
    public String getSiteForHostname(String hostname) {
        Site s =  siteRepository.findByHostname(hostname.toLowerCase());
        if (s != null) {
            return s.name;
        }
        return siteRepository.findByHostname("*").name;
    }
}
