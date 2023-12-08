package us.calubrecht.lazerwiki.responses;

import us.calubrecht.lazerwiki.model.MediaRecord;

import java.util.List;
import java.util.Map;

public class MediaListResponse {
    final public Map<String, List<MediaRecord>> media;

    final public NsNode namespaces;

    public MediaListResponse(Map<String, List<MediaRecord>> media, NsNode namespaces) {
        this.media = media;
        this.namespaces = namespaces;
    }
}
