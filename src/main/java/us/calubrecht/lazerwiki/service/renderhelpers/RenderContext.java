package us.calubrecht.lazerwiki.service.renderhelpers;

import us.calubrecht.lazerwiki.service.IMarkupRenderer;

import java.util.HashMap;
import java.util.Map;

public record RenderContext(String host, String site, String page, String user, IMarkupRenderer renderer, Map<String, Object> renderState) {

    // For tests that don't need renderer in context (not using macros)
    public RenderContext(String host, String site, String page, String user) {
        this(host, site, page, user, null, new HashMap<>());
    }

}
