package us.calubrecht.lazerwiki.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import us.calubrecht.lazerwiki.model.PageData;
import us.calubrecht.lazerwiki.service.PageService;
import us.calubrecht.lazerwiki.service.RenderService;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("api/page/")
public class PageController {
    @Autowired
    PageService pageService;

    @Autowired
    RenderService renderService;

    @RequestMapping(value = {"{pageDescriptor}", ""})
    public PageData getPage(@PathVariable Optional<String> pageDescriptor, Principal principal, HttpServletRequest request ) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal == null ? "Guest" : principal.getName();
        return renderService.getRenderedPage(url.getHost(), pageDescriptor.orElse(""), userName);
    }

    @PostMapping("{pageDescriptor}/savePage")
    public PageData savePage(@PathVariable String pageDescriptor, Principal principal, HttpServletRequest request, @RequestBody Map<String, String> body) throws MalformedURLException, PageWriteException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal.getName();
        pageService.savePage(url.getHost(), pageDescriptor, body.get("text"), userName);
        return renderService.getRenderedPage(url.getHost(), pageDescriptor, userName);
    }

}
