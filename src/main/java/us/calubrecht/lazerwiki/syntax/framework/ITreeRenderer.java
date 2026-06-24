package us.calubrecht.lazerwiki.syntax.framework;

import static us.calubrecht.lazerwiki.model.RenderResult.RenderStateKeys.ERRORS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.text.StringEscapeUtils;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

public interface ITreeRenderer {
  Collection<Class<? extends ITreeNode>> getTargets();

  void setRegistrar(ParserRegistrar registrar);

  StringBuilder renderHtml(ITreeNode node, RenderContext renderContext);

  StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext);

  class DefaultRenderer implements ITreeRenderer {

    @Override
    public Collection<Class<? extends ITreeNode>> getTargets() {
      return List.of();
    }

    @Override
    public void setRegistrar(ParserRegistrar registrar) {}

    @Override
    @SuppressWarnings("unchecked")
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
      String text = node.asString();
      if (text.contains("<script>")) {
        String error =
            String.format(
                "Suspicious text at %s. Raw text =[%s]", node.getPosition().getLeft(), text);
        ((List<String>)
                renderContext
                    .renderState()
                    .computeIfAbsent(ERRORS.name(), (k) -> new ArrayList<>()))
            .add(error);
      }
      return new StringBuilder(sanitizeLeaveQuotes(text));
    }

    @Override
    public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
      return new StringBuilder(node.asString());
    }
  }

  /**
   * Sanitize user input to escape html elements. Warn: DOES NOT escape quotes, do not use as part
   * of propertie of an html tag
   */
  default String sanitizeLeaveQuotes(String input) {
    return sanitize(input).replace("&quot;", "\"");
  }

  /** Sanitize user input to escape html elements. */
  default String sanitize(String input) {
    return StringEscapeUtils.escapeHtml4(input);
  }
}
