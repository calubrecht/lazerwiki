package us.calubrecht.lazerwiki.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import us.calubrecht.lazerwiki.service.PageService;

import java.net.MalformedURLException;
import java.net.URL;

@RestController
@RequestMapping("api/page/")
public class PageController {
    @Autowired
    PageService pageService;

    @RequestMapping("{pageDescriptor}")
    public String getPage(@PathVariable String pageDescriptor, HttpServletRequest request ) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        return pageService.getSource(url.getHost(), pageDescriptor);
    }
}
