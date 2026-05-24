package us.calubrecht.lazerwiki.responses;

import us.calubrecht.lazerwiki.model.MediaRecord;

import java.util.List;
import java.util.Map;

public record MediaListResponse(Map<String, List<MediaRecord>> media, NsNode namespaces) {
}
