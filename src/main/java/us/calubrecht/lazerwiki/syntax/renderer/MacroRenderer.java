package us.calubrecht.lazerwiki.syntax.renderer;

import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.MacroService;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.MacroNode;

@Component("customSynMacroRenderer")
public class MacroRenderer extends AbstractRenderer {
  final MacroService macroService;

  public MacroRenderer(@Autowired MacroService macroService) {
    this.macroService = macroService;
  }

  @Override
  public Collection<Class<? extends ITreeNode>> getTargets() {
    return List.of(MacroNode.class);
  }

  @Override
  public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
    MacroNode macro = (MacroNode) node;
    return new StringBuilder(
        macroService.renderMacro(macro.getMacroText(), macro.getMacroFullText(), renderContext));
  }

  @Override
  public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
    return new StringBuilder();
  }
}
