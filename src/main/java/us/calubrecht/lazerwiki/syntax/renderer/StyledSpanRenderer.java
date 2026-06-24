package us.calubrecht.lazerwiki.syntax.renderer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.StyledSpanNode;
import us.calubrecht.lazerwiki.syntax.nodes.StyledSpanNode.SpanType;

@Component
public class StyledSpanRenderer extends ContainerRenderer {
  final Map<SpanType, String> cssClassMap =
      Map.of(
          SpanType.BOLD,
          "bold",
          SpanType.ITALIC,
          "italic",
          SpanType.UNDERLINE,
          "underline",
          SpanType.MONOSPACE,
          "monospace");

  @Override
  public Collection<Class<? extends ITreeNode>> getTargets() {
    return List.of(StyledSpanNode.class);
  }

  @Override
  public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
    StyledSpanNode spanNode = (StyledSpanNode) node;
    StringBuilder outBuffer = new StringBuilder();
    outBuffer.append("<span class=\"").append(cssClassMap.get(spanNode.getType())).append("\">");
    outBuffer.append(super.renderHtml(node, renderContext).toString());
    outBuffer.append("</span>");
    return outBuffer;
  }
}
