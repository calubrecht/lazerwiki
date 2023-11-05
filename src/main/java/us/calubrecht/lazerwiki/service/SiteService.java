package us.calubrecht.lazerwiki.service;

import org.springframework.stereotype.Service;

@Service
public class SiteService {
    public String getSiteForHostname(String hostname) {
        return "default";
    }
}
