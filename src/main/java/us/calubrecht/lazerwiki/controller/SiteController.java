package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.calubrecht.lazerwiki.service.SiteService;

import java.net.MalformedURLException;
import java.net.URL;

@RestController
@RequestMapping("api/site/")
public class SiteController {
    @Autowired
    SiteService siteService;

    @GetMapping
    public String getSiteName(HttpServletRequest request) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        return siteService.getSiteNameForHostname(url.getHost());
    }


}
