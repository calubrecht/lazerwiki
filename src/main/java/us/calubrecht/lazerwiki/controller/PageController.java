package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.requests.MovePageRequest;
import us.calubrecht.lazerwiki.requests.SavePageRequest;
import us.calubrecht.lazerwiki.responses.*;
import us.calubrecht.lazerwiki.service.*;
import us.calubrecht.lazerwiki.service.exception.PageReadException;
import us.calubrecht.lazerwiki.service.exception.PageRevisionException;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

@RestController
@RequestMapping(value = {"api/page/", "app/api/page/"})
public class PageController extends LazerWikiController {
  @Autowired PageService pageService;

  @Autowired PageSearchService pageSearchService;

  @Autowired PageUpdateService pageUpdateService;

  @Autowired RenderService renderService;

  @Autowired PageLockService pageLockService;

  @Autowired UserService userService;

  final Logger logger = LogManager.getLogger(getClass());

  @RequestMapping(value = {"/get/{pageDescriptor}", "/get/"})
  public PageData getPage(
      @PathVariable Optional<String> pageDescriptor,
      Principal principal,
      HttpServletRequest request)
      throws MalformedURLException {
    String userName = getUsername(principal);
    PerfTracker tracker = new PerfTracker();
    PageData pd =
        renderService.getRenderedPage(
            getSite(request), pageDescriptor.orElse(""), userName, tracker);
    tracker.stopAll();
    return pd;
  }

  private static String getUsername(Principal principal) {
    return principal == null ? User.GUEST : principal.getName();
  }

  @RequestMapping(value = {"/history/{pageDescriptor}", "/history/"})
  public ResponseEntity<List<PageDesc>> getPageHistory(
      @PathVariable Optional<String> pageDescriptor,
      Principal principal,
      HttpServletRequest request)
      throws MalformedURLException {
    String userName = getUsername(principal);
    try {
      return ResponseEntity.ok(
          pageService.getPageHistory(
              getSite(request), pageDescriptor.orElse(""), userName));
    } catch (PageReadException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
  }

  @RequestMapping(
      value = {"/getHistorical/{pageDescriptor}/{revision}", "/getHistorical/{revision}"})
  public PageData getPageHistorical(
      @PathVariable Optional<String> pageDescriptor,
      @PathVariable long revision,
      Principal principal,
      HttpServletRequest request)
      throws MalformedURLException {
    String userName = getUsername(principal);
    return renderService.getHistoricalRenderedPage(
        getSite(request), pageDescriptor.orElse(""), revision, userName);
  }

  @RequestMapping(value = {"/diff/{pageDescriptor}/{rev1}/{rev2}", "/diff/{rev1}/{rev2}"})
  public ResponseEntity<List<Pair<Integer, String>>> getPageDiff(
      @PathVariable Optional<String> pageDescriptor,
      @PathVariable Long rev1,
      @PathVariable Long rev2,
      Principal principal,
      HttpServletRequest request)
      throws MalformedURLException {
    String userName = getUsername(principal);
    try {
      return ResponseEntity.ok(
          pageService.getPageDiff(
              getSite(request), pageDescriptor.orElse(""), rev1, rev2, userName));
    } catch (PageReadException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
  }

  @PostMapping(value = {"/savePage", "{pageDescriptor}/savePage"})
  public PageData savePage(
      @PathVariable Optional<String> pageDescriptor,
      Principal principal,
      HttpServletRequest request,
      @RequestBody SavePageRequest body)
      throws MalformedURLException, PageWriteException {
    String site = getSite(request);
    String userName = getUsername(principal);
    if (userName.equals(User.GUEST)) {
      logger.info(
          "Guest user {} saving page {}", request.getRemoteAddr(), pageDescriptor.orElse("ROOT"));
    }
    try {
      PerfTracker tracker = new PerfTracker();
      renderService.savePage(
          site,
          pageDescriptor.orElse(""),
          body.getText(),
          body.getTags(),
          body.getRevision(),
          body.isForce(),
          userName);
      PageData pd =
          renderService.getRenderedPage(site, pageDescriptor.orElse(""), userName, tracker);
      tracker.stopAll();
      return pd;
    } catch (PageRevisionException pre) {
      return new PageData(null, null, null, null, null, null, null, null, false, pre.getMessage());
    }
  }

  @DeleteMapping("{pageDescriptor}")
  public void deletePage(
      @PathVariable String pageDescriptor, Principal principal, HttpServletRequest request)
      throws MalformedURLException, PageWriteException {
    String userName = principal.getName();
    pageUpdateService.deletePage(getSite(request), pageDescriptor, userName);
  }

  @RequestMapping(value = "/listPages")
  public PageListResponse listPages(Principal principal, HttpServletRequest request)
      throws MalformedURLException {
    String userName = getUsername(principal);
    return pageService.getAllPages(getSite(request), userName);
  }

  @RequestMapping(value = "/listNamespaces/{site}")
  public ResponseEntity<PageListResponse> listNamespaces(
      Principal principal, @PathVariable("site") String site, HttpServletRequest request) {
    String userName = getUsername(principal);
    User user = userService.getUser(userName);
    if (!user.getRolesString().contains("ROLE_ADMIN")
        && !user.getRolesString().contains("ROLE_ADMIN:" + site)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    return ResponseEntity.ok(pageService.getAllNamespaces(site, userName));
  }

  @RequestMapping(value = "/recentChanges")
  public RecentChangesResponse recentChanges(Principal principal, HttpServletRequest request)
      throws MalformedURLException {
    String userName = getUsername(principal);
    return pageService.recentChanges(getSite(request), userName);
  }

  @RequestMapping(value = "/searchPages")
  public Map<String, List<SearchResult>> searchPages(
      Principal principal, HttpServletRequest request, @RequestParam("search") String searchTerm)
      throws MalformedURLException {
    String userName = getUsername(principal);
    return pageSearchService.searchPages(getSite(request), userName, searchTerm);
  }

  @RequestMapping(value = "/listTags")
  public List<String> listTags(Principal principal, HttpServletRequest request)
      throws MalformedURLException {
    String userName = getUsername(principal);
    return pageService.getAllTags(getSite(request), userName);
  }

  @PostMapping(value = {"/previewPage/{pageDescriptor}", "/previewPage/"})
  public PageData previewPage(
      @PathVariable Optional<String> pageDescriptor,
      Principal principal,
      HttpServletRequest request,
      @RequestBody SavePageRequest body)
      throws MalformedURLException {
    String userName = principal.getName();
    PerfTracker tracker = new PerfTracker();
    return renderService.previewPage(
        getSite(request), pageDescriptor.orElse(""), body.getText(), userName, tracker);
  }

  @PostMapping(value = {"/lock/{pageDescriptor}", "/lock/"})
  public PageLockResponse lockPage(
      @PathVariable Optional<String> pageDescriptor,
      Principal principal,
      HttpServletRequest request,
      @RequestParam Optional<Boolean> overrideLock)
      throws MalformedURLException {
    String userName = getUsername(principal);
    return pageLockService.getPageLock(
        getSite(request), pageDescriptor.orElse(""), userName, overrideLock.orElse(false));
  }

  @PostMapping(value = {"/releaseLock/{pageDescriptor}/id/{lock}", "/releaseLock/id/{lock}"})
  public void releaseLock(
      @PathVariable Optional<String> pageDescriptor,
      Principal principal,
      @PathVariable String lock,
      HttpServletRequest request)
      throws MalformedURLException {
    String userName = getUsername(principal);
    pageLockService.releasePageLock(
        getSite(request), pageDescriptor.orElse(""), lock, userName);
  }

  @PostMapping(value = {"/{pageDescriptor}/movePage"})
  public MoveStatus movePage(
      @PathVariable String pageDescriptor,
      Principal principal,
      HttpServletRequest request,
      @RequestBody MovePageRequest movePageRequest)
      throws MalformedURLException, PageWriteException {
    String userName = principal.getName();
    return pageUpdateService.movePage(
        getSite(request),
        userName,
        movePageRequest.oldNS(),
        movePageRequest.oldPage(),
        movePageRequest.newNS(),
        movePageRequest.newPage());
  }
}
