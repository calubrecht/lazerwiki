package us.calubrecht.lazerwiki.responses;

import java.util.List;
import java.util.Map;
import us.calubrecht.lazerwiki.model.PageDesc;

public record PageListResponse(Map<String, List<PageDesc>> pages, NsNode namespaces) {}
