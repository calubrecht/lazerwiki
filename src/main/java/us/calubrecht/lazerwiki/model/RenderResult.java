package us.calubrecht.lazerwiki.model;

import java.util.Map;

public record RenderResult(
    String renderedText, String plainText, Map<RenderStateKeys, Object> renderState) {

  public String getTitle() {
    Object title = renderState().get(RenderStateKeys.TITLE);
    return title != null ? title.toString() : null;
  }
}
