package us.calubrecht.lazerwiki.responses;

import us.calubrecht.lazerwiki.model.PageDesc;

import java.util.List;
import java.util.Map;

public record PageListResponse(Map<String, List<PageDesc>> pages, NsNode namespaces) {
}
