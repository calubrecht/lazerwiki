package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.calubrecht.lazerwiki.model.MediaHistoryRecord;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.model.RecentChangesResponse;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.service.MediaService;
import us.calubrecht.lazerwiki.service.PageService;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("api/history/")
public class HistoryController {
    @Autowired
    PageService pageService;

    @Autowired
    MediaService mediaService;

    @RequestMapping(value = "/recentChanges")
    public RecentChangesResponse recentChanges(Principal principal, HttpServletRequest request) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String userName = principal == null ? User.GUEST : principal.getName();
        RecentChangesResponse pageChanges = pageService.recentChanges(url.getHost(), userName);
        List<PageDesc> desc = pageChanges.changes().stream().map(rcr -> rcr.pageDesc()).toList();
        List<MediaHistoryRecord> mediaChanges = mediaService.getRecentChanges(url.getHost(), userName);
        List<Object> merged = mergePageAndMedia(pageChanges.changes(), mediaChanges);
        return new RecentChangesResponse(pageChanges.changes(), mediaChanges, merged);
    }

    List<Object> mergePageAndMedia(List<RecentChangesResponse.RecentChangeRec> pages, List<MediaHistoryRecord> mediaChanges) {
        if (pages.isEmpty() && mediaChanges.isEmpty()) {
            return Collections.emptyList();
        }
        if (pages.isEmpty()) {
            return new ArrayList<>(mediaChanges);
        }
        if (mediaChanges.isEmpty()) {
            return new ArrayList<>(pages);
        }
        List<RecentChangesResponse.RecentChangeRec> pagesStack = new ArrayList<>(pages);
        List<MediaHistoryRecord> mediaStack = new ArrayList<>(mediaChanges);
        List<Object> merged = new ArrayList<>();
        int count = 0;
        while (count < 10 && !pagesStack.isEmpty() && !mediaStack.isEmpty()) {
            if (pagesStack.get(0).pageDesc().getModified().isAfter(
                    mediaStack.get(0).getTs()
            )) {
                merged.add(pagesStack.get(0));
                pagesStack.remove(0);
            }
            else {
                merged.add(mediaStack.get(0));
                mediaStack.remove(0);
            }
            count++;
        }
        if (count < 10) {
            merged.addAll(pagesStack);
            merged.addAll(mediaStack);
            return merged.subList(0, Math.min(10, merged.size()));
        }
        return merged;
    }

}
