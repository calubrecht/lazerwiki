package us.calubrecht.lazerwiki.syntax.renderer;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.CodeBlockNode;

import java.util.Collection;
import java.util.List;

@Component
public class CodeBlockRenderer extends ContainerRenderer {
    @Override
    public Collection<Class<? extends ITreeNode>> getTargets() {
        return List.of(CodeBlockNode.class);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<pre class=\"code\">");
        buffer.append(super.renderHtml(node, renderContext));
        buffer.append("</pre>");
        return buffer;
    }
}
