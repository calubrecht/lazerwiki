package us.calubrecht.lazerwiki.model;

import org.jsoup.nodes.DocumentType;

import java.util.List;
import java.util.Map;

public record RenderResult (String renderedText, String plainText, Map<String, Object> renderState) {

    public enum RENDER_STATE_KEYS {TITLE, LINKS, IMAGES, DONT_CACHE, FOR_CACHE}

    public String getTitle() {
        Object title = renderState().get(RENDER_STATE_KEYS.TITLE.name());
        return title != null ? title.toString() : null;
    }
}
