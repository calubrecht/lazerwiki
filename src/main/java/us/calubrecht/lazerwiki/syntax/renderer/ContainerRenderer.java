package us.calubrecht.lazerwiki.syntax.renderer;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.ContainerNode;
import us.calubrecht.lazerwiki.syntax.nodes.ListItemNode;
import us.calubrecht.lazerwiki.syntax.nodes.UnformatSpanNode;

import java.util.Collection;
import java.util.List;

@Component
public class ContainerRenderer  extends AbstractRenderer{
    @Override
    public Collection<Class> getTargets() {
        return List.of(ContainerNode.class, UnformatSpanNode.class);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        ContainerNode container = (ContainerNode)node;
        StringBuilder buffer = new StringBuilder();
        for(ITreeNode child : container.getChildren()) {
            buffer.append(parserRegistrar.getRenderer(child.getClass()).renderHtml(child, renderContext));
        }
        return buffer;
    }

    @Override
    public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
        ContainerNode container = (ContainerNode)node;
        StringBuilder buffer = new StringBuilder();
        for(ITreeNode child : container.getChildren()) {
            buffer.append(parserRegistrar.getRenderer(child.getClass()).renderPlaintext(child, renderContext));
        }
        return buffer;
    }
}
