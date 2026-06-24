package us.calubrecht.lazerwiki.model;

import java.util.Map;

public record RenderResult(String renderedText, String plainText, Map<String, Object> renderState) {

  public enum RenderStateKeys {
    TITLE,
    LINKS,
    IMAGES,
    HEADERS,
    OVERRIDE_STATS,
    LINK_OVERRIDES,
    MEDIA_OVERRIDES,
    TOC,
    DONT_CACHE,
    FOR_CACHE,
    ID_SUFFIX,
    ERRORS
  }

  public String getTitle() {
    Object title = renderState().get(RenderStateKeys.TITLE.name());
    return title != null ? title.toString() : null;
  }
}
