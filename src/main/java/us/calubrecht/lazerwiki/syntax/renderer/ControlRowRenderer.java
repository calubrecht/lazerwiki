package us.calubrecht.lazerwiki.syntax.renderer;

import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.ControlRowNode;

/** Not a real renderer, Control Rows are never rendered, but pass data for post-processing */
@Component("customSynControlRowRenderer")
public class ControlRowRenderer extends AbstractRenderer {
  @Override
  public Collection<Class<? extends ITreeNode>> getTargets() {
    return List.of(ControlRowNode.class);
  }

  @Override
  public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
    ControlRowNode controlRowNode = (ControlRowNode) node;
    String token = controlRowNode.getToken();
    if (token.equals("~~YESTOC~~")) {
      renderContext.renderState().put(RenderResult.RenderStateKeys.TOC.name(), true);
    } else // if (!context.NO_TOC_TOKEN().isEmpty()) {
    {
      // ~~NOTOC~~
      renderContext.renderState().put(RenderResult.RenderStateKeys.TOC.name(), false);
    }
    return new StringBuilder();
  }

  @Override
  public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
    return new StringBuilder();
  }
}
