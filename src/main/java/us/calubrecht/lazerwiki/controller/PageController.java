package us.calubrecht.lazerwiki.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.responses.PageListResponse;
import us.calubrecht.lazerwiki.requests.SavePageRequest;
import us.calubrecht.lazerwiki.responses.SearchResult;
import us.calubrecht.lazerwiki.service.PageService;
import us.calubrecht.lazerwiki.service.PageUpdateService;
import us.calubrecht.lazerwiki.service.RenderService;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("api/page/")
public class PageController {
    @Autowired
    PageService pageService;

    @Autowired
    PageUpdateService pageUpdateService;

    @Autowired
    RenderService renderService;

    @RequestMapping(value = {"/get/{pageDescriptor}", "/get/"})
    public PageData getPage(@PathVariable Optional<String> pageDescriptor, Principal principal, HttpServletRequest request ) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal == null ? "Guest" : principal.getName();
        return renderService.getRenderedPage(url.getHost(), pageDescriptor.orElse(""), userName);
    }

    @PostMapping(value = { "/savePage", "{pageDescriptor}/savePage"})
    public PageData savePage(@PathVariable Optional<String> pageDescriptor, Principal principal, HttpServletRequest request, @RequestBody SavePageRequest body) throws MalformedURLException, PageWriteException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal.getName();
        renderService.savePage(url.getHost(), pageDescriptor.orElse(""), body.getText(), body.getTags(), userName);
        return renderService.getRenderedPage(url.getHost(), pageDescriptor.orElse(""), userName);
    }

    @DeleteMapping("{pageDescriptor}")
    public void deletePage(@PathVariable String pageDescriptor, Principal principal, HttpServletRequest request) throws MalformedURLException, PageWriteException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal.getName();
        pageUpdateService.deletePage(url.getHost(), pageDescriptor, userName);
    }

    @RequestMapping(value = "/listPages")
    public PageListResponse listPages(Principal principal, HttpServletRequest request) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal == null ? User.GUEST : principal.getName();
        return pageService.getAllPages(url.getHost(), userName);
    }

    @RequestMapping(value = "/searchPages")
    public Map<String, List<SearchResult>> searchPages(Principal principal, HttpServletRequest request, @RequestParam("search") String searchTerm) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal == null ? User.GUEST : principal.getName();
        return pageService.searchPages(url.getHost(), userName, searchTerm);
    }

    @RequestMapping(value = "/listTags")
    public List<String> listTags(Principal principal, HttpServletRequest request) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal == null ? User.GUEST : principal.getName();
        return pageService.getAllTags(url.getHost(), userName);
    }

    @PostMapping(value = {"/previewPage/{pageDescriptor}", "/previewPage/"})
    public PageData previewPage(@PathVariable Optional<String> pageDescriptor, Principal principal, HttpServletRequest request, @RequestBody SavePageRequest body) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal.getName();
        return renderService.previewPage(url.getHost(), pageDescriptor.orElse(""), body.getText(), userName);
    }
}
