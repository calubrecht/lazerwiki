package us.calubrecht.lazerwiki.syntax.renderer;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.LineBreakNode;

import java.util.Collection;
import java.util.List;

@Component("customSynLineBreakRenderer")
public class LineBreakRenderer extends AbstractRenderer {
    @Override
    public Collection<Class<? extends ITreeNode>> getTargets() {
        return List.of(LineBreakNode.class);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        return new StringBuilder("<br>");
    }

    @Override
    public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
        return new StringBuilder("\n");
    }
}
