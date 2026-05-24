package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.calubrecht.lazerwiki.service.SitemapService;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

@RestController
public class SitemapController {
    @Autowired
    SitemapService service;

    @RequestMapping(value = "/sitemap.xml", produces= MediaType.APPLICATION_XML_VALUE)
    ResponseEntity<String> sitemap(HttpServletRequest request) throws MalformedURLException, ParseException {
        URL url = new URL(request.getRequestURL().toString());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Disposition", "attachment;filename=sitemap.xml");
        return new ResponseEntity<>(service.getSitemap(url), headers, HttpStatus.OK);
    }
}
