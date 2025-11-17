package us.calubrecht.lazerwiki.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.model.RecentChangesResponse;
import us.calubrecht.lazerwiki.requests.MovePageRequest;
import us.calubrecht.lazerwiki.responses.*;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.requests.SavePageRequest;
import us.calubrecht.lazerwiki.service.PageLockService;
import us.calubrecht.lazerwiki.service.PageService;
import us.calubrecht.lazerwiki.service.PageUpdateService;
import us.calubrecht.lazerwiki.service.RenderService;
import us.calubrecht.lazerwiki.service.exception.PageReadException;
import us.calubrecht.lazerwiki.service.exception.PageRevisionException;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value={"api/page/", "app/api/page/"})
public class PageController {
    @Autowired
    PageService pageService;

    @Autowired
    PageUpdateService pageUpdateService;

    @Autowired
    RenderService renderService;

    @Autowired
    PageLockService pageLockService;

    @RequestMapping(value = {"/get/{pageDescriptor}", "/get/"})
    public PageData getPage(@PathVariable Optional<String> pageDescriptor, Principal principal, HttpServletRequest request ) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = getUsername(principal);
        return renderService.getRenderedPage(url.getHost(), pageDescriptor.orElse(""), userName);
    }

    private static String getUsername(Principal principal) {
        return principal == null ? User.GUEST : principal.getName();
    }

    @RequestMapping(value = {"/history/{pageDescriptor}", "/history/"})
    public ResponseEntity<List<PageDesc>> getPageHistory(@PathVariable Optional<String> pageDescriptor, Principal principal, HttpServletRequest request ) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = getUsername(principal);
        try {
            return ResponseEntity.ok(pageService.getPageHistory(url.getHost(), pageDescriptor.orElse(""), userName));
        } catch (PageReadException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @RequestMapping(value = {"/getHistorical/{pageDescriptor}/{revision}", "/getHistorical/{revision}"})
    public PageData getPageHistorical(@PathVariable Optional<String> pageDescriptor, @PathVariable long revision, Principal principal, HttpServletRequest request ) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = getUsername(principal);
        return renderService.getHistoricalRenderedPage(url.getHost(), pageDescriptor.orElse(""), revision, userName);
    }

    @RequestMapping(value = {"/diff/{pageDescriptor}/{rev1}/{rev2}", "/diff/{rev1}/{rev2}"})
    public ResponseEntity<List<Pair<Integer,String>>> getPageDiff(@PathVariable Optional<String> pageDescriptor, @PathVariable Long rev1, @PathVariable Long rev2, Principal principal, HttpServletRequest request ) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = getUsername(principal);
        try {
            return ResponseEntity.ok(pageService.getPageDiff(url.getHost(), pageDescriptor.orElse(""), rev1, rev2, userName));
        } catch (PageReadException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping(value = { "/savePage", "{pageDescriptor}/savePage"})
    public PageData savePage(@PathVariable Optional<String> pageDescriptor, Principal principal, HttpServletRequest request, @RequestBody SavePageRequest body) throws MalformedURLException, PageWriteException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = getUsername(principal);
        try {
            renderService.savePage(url.getHost(), pageDescriptor.orElse(""), body.getText(), body.getTags(), body.getRevision(), body.isForce(), userName);
            return renderService.getRenderedPage(url.getHost(), pageDescriptor.orElse(""), userName);
        }
        catch (PageRevisionException pre) {
            return new PageData(null, null, null, null, null, null, null, null, false, pre.getMessage());
        }
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
        String userName = getUsername(principal);
        return pageService.getAllPages(url.getHost(), userName);
    }

    @RequestMapping(value = "/listNamespaces/{site}")
    public PageListResponse listNamespaces(Principal principal, @PathVariable("site") String site, HttpServletRequest request) throws MalformedURLException {
        String userName = getUsername(principal);
        return pageService.getAllNamespaces(site, userName);
    }

    @RequestMapping(value = "/recentChanges")
    public RecentChangesResponse recentChanges(Principal principal, HttpServletRequest request) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = getUsername(principal);
        return pageService.recentChanges(url.getHost(), userName);
    }

    @RequestMapping(value = "/searchPages")
    public Map<String, List<SearchResult>> searchPages(Principal principal, HttpServletRequest request, @RequestParam("search") String searchTerm) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = getUsername(principal);
        return pageService.searchPages(url.getHost(), userName, searchTerm);
    }

    @RequestMapping(value = "/listTags")
    public List<String> listTags(Principal principal, HttpServletRequest request) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = getUsername(principal);
        return pageService.getAllTags(url.getHost(), userName);
    }

    @PostMapping(value = {"/previewPage/{pageDescriptor}", "/previewPage/"})
    public PageData previewPage(@PathVariable Optional<String> pageDescriptor, Principal principal, HttpServletRequest request, @RequestBody SavePageRequest body) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal.getName();
        return renderService.previewPage(url.getHost(), pageDescriptor.orElse(""), body.getText(), userName);
    }

    @PostMapping(value = {"/lock/{pageDescriptor}", "/lock/"})
    public PageLockResponse lockPage(@PathVariable Optional<String> pageDescriptor, Principal principal, HttpServletRequest request, @RequestParam Optional<Boolean> overrideLock) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = getUsername(principal);
        return pageLockService.getPageLock(url.getHost(), pageDescriptor.orElse(""), userName, overrideLock.orElse(false));
    }

    @PostMapping(value = {"/releaseLock/{pageDescriptor}/id/{lock}", "/releaseLock/id/{lock}"})
    public void releaseLock(@PathVariable Optional<String> pageDescriptor, @PathVariable String lock, HttpServletRequest request) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        pageLockService.releasePageLock(url.getHost(), pageDescriptor.orElse(""), lock);
    }

    @PostMapping(value = {"/{pageDescriptor}/movePage"})
    public MoveStatus movePage(@PathVariable String pageDescriptor, Principal principal, HttpServletRequest request, @RequestBody MovePageRequest movePageRequest) throws MalformedURLException, PageWriteException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal.getName();
        return pageUpdateService.movePage(url.getHost(), userName, movePageRequest.oldNS(), movePageRequest.oldPage(), movePageRequest.newNS(), movePageRequest.newPage());
    }
}
