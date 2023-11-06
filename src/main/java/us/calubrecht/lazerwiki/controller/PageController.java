package us.calubrecht.lazerwiki.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import us.calubrecht.lazerwiki.service.PageService;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;

@RestController
@RequestMapping("api/page/")
public class PageController {
    @Autowired
    PageService pageService;

    @RequestMapping("{pageDescriptor}")
    public String getPage(@PathVariable String pageDescriptor, Principal principal, HttpServletRequest request ) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal == null ? "Guest" : principal.getName();
        return pageService.getSource(url.getHost(), pageDescriptor, userName);
    }

    @RequestMapping("{pageDescriptor}/source")
    public String getPageSource(@PathVariable String pageDescriptor, Principal principal, HttpServletRequest request ) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal == null ? "Guest" : principal.getName();
        return pageService.getSource(url.getHost(), pageDescriptor, userName);
    }

}
