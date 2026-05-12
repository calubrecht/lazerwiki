package us.calubrecht.lazerwiki.syntax.renderer;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.ParagraphNode;

import java.util.Collection;
import java.util.List;

@Component
public class ParagraphRenderer extends ContainerRenderer {

    @Override
    public Collection<Class> getTargets() {
        return List.of(ParagraphNode.class);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<div>");
        buffer.append(super.renderHtml(node, renderContext));
        buffer.append("</div>");
        return buffer;
    }
}