package us.calubrecht.lazerwiki.syntax.renderer;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.HorizontalRuleNode;

import java.util.Collection;
import java.util.List;

@Component("customSynHorizontalRuleRenderer")
public class HorizontalRuleRenderer extends AbstractRenderer {
    @Override
    public Collection<Class<? extends ITreeNode>> getTargets() {
        return List.of(HorizontalRuleNode.class);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        return new StringBuilder("<hr>");
    }

    @Override
    public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
        HorizontalRuleNode hr = (HorizontalRuleNode)node;
        return new StringBuilder(hr.getSource());
    }
}
