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
    public Collection<Class<? extends ITreeNode>> getTargets() {
        return List.of(ParagraphNode.class);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        StringBuilder content = super.renderHtml(node, renderContext);
        if (content.isEmpty()) {
            return new StringBuilder();
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append("<div>");
        buffer.append(content);
        buffer.append("</div>\n");
        return buffer;
    }
}