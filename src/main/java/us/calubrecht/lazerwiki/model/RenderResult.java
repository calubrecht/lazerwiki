package us.calubrecht.lazerwiki.model;

import java.util.List;
import java.util.Map;

public record RenderResult (String renderedText, Map<String, Object> renderState) {

    public enum RENDER_STATE_KEYS {TITLE};

    public String getTitle() {
        Object title = renderState().get(RENDER_STATE_KEYS.TITLE.name());
        return title != null ? title.toString() : null;
    }
}
