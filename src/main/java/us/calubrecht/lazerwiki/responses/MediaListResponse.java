package us.calubrecht.lazerwiki.responses;

import java.util.List;
import java.util.Map;
import us.calubrecht.lazerwiki.model.MediaRecord;

public record MediaListResponse(Map<String, List<MediaRecord>> media, NsNode namespaces) {}
