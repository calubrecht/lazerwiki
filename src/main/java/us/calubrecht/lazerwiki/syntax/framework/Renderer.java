package us.calubrecht.lazerwiki.syntax.framework;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

@Component
public class Renderer {
  final ParserRegistrar parserRegistrar;

  public Renderer(@Autowired ParserRegistrar parserRegistrar) {
    this.parserRegistrar = parserRegistrar;
  }

  public String render(ITreeNode node, RenderContext renderContext) {
    ITreeRenderer renderer = parserRegistrar.getRenderer(node.getClass());
    return renderer.renderHtml(node, renderContext).toString().strip();
  }

  public String renderPlaintext(ITreeNode node, RenderContext renderContext) {
    ITreeRenderer renderer = parserRegistrar.getRenderer(node.getClass());
    return renderer.renderPlaintext(node, renderContext).toString().stripTrailing();
  }
}
