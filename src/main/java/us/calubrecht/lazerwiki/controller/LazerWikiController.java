package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import us.calubrecht.lazerwiki.service.SiteService;

public abstract class LazerWikiController {

  @Autowired SiteService siteService;

  protected String getHost(HttpServletRequest request) throws MalformedURLException {
    return URI.create(request.getRequestURL().toString()).toURL().getHost();
  }

  protected String getSite(HttpServletRequest request) throws MalformedURLException {
    return siteService.getSiteForHostname(getHost(request));
  }
}
