package us.calubrecht.lazerwiki.model;

import java.util.List;
import java.util.Map;

public class MediaListResponse {
    public Map<String, List<MediaRecord>> media;

    public NsNode namespaces;

    public MediaListResponse(Map<String, List<MediaRecord>> media, NsNode namespaces) {
        this.media = media;
        this.namespaces = namespaces;
    }
}
