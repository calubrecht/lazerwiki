package us.calubrecht.lazerwiki.syntax.renderer;

import static us.calubrecht.lazerwiki.model.RenderResult.RenderStateKeys.HEADERS;
import static us.calubrecht.lazerwiki.model.RenderResult.RenderStateKeys.ID_SUFFIX;

import java.util.*;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.model.HeaderRef;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.HeaderNode;

@Component("customSynHeaderRenderer")
public class HeaderRenderer extends ContainerRenderer {
  @Override
  public Collection<Class<? extends ITreeNode>> getTargets() {
    return List.of(HeaderNode.class);
  }

  @Override
  public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
    HeaderNode headerNode = (HeaderNode) node;
    String hTag = "h" + headerNode.getLevel();
    String plainTextHeader = renderPlaintext(node, renderContext).toString().strip();
    @SuppressWarnings("unchecked")
    List<HeaderRef> headers =
        ((List<HeaderRef>)
            renderContext.renderState().computeIfAbsent(HEADERS.name(), (k) -> new ArrayList<>()));
    HeaderRef headerRef =
        new HeaderRef(headerNode.getLevel(), plainTextHeader, toId(plainTextHeader, headers));
    headers.add(headerRef);
    StringBuilder outBuffer = new StringBuilder();
    String id =
        headerRef.id() + renderContext.renderState().getOrDefault(ID_SUFFIX.name(), "").toString();
    outBuffer.append("<").append(hTag).append(" id=\"").append(id).append("\">");
    outBuffer.append(super.renderHtml(node, renderContext).toString().strip());
    outBuffer.append("</").append(hTag).append(">\n");
    if (!renderContext.renderState().containsKey(RenderResult.RenderStateKeys.TITLE.name())) {
      renderContext
          .renderState()
          .put(
              RenderResult.RenderStateKeys.TITLE.name(),
              renderPlaintext(node, renderContext).toString().strip());
    }
    return outBuffer;
  }

  @Override
  public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
    return super.renderPlaintext(node, renderContext).append("\n");
  }

  String toId(String headerText, List<HeaderRef> existingHeaders) {
    String id = "header_" + headerText.replaceAll("[^a-zA-Z_0-9\\-:.]", "_");
    if (existingHeaders.stream().anyMatch(r -> r.id().equals(id))) {
      return id + "_1";
    }
    return id;
  }
}
