package us.calubrecht.lazerwiki.responses;

import us.calubrecht.lazerwiki.model.PageDesc;

import java.util.List;
import java.util.Map;

public class PageListResponse {
    public Map<String, List<PageDesc>> pages;

    public NsNode namespaces;

    public PageListResponse(Map<String, List<PageDesc>> pages, NsNode namespaces) {
        this.pages = pages;
        this.namespaces = namespaces;
    }
}
