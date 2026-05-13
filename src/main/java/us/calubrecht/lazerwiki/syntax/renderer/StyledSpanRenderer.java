package us.calubrecht.lazerwiki.syntax.renderer;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.StyledSpanNode;
import us.calubrecht.lazerwiki.syntax.nodes.StyledSpanNode.SPAN_TYPE;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class StyledSpanRenderer extends ContainerRenderer {
    final Map<SPAN_TYPE, String> cssClassMap =
            Map.of(SPAN_TYPE.BOLD, "bold",
                SPAN_TYPE.ITALIC, "italic",
                SPAN_TYPE.UNDERLINE, "underline");
    @Override
    public Collection<Class> getTargets() {
        return List.of(StyledSpanNode.class);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        StyledSpanNode spanNode = (StyledSpanNode)node;
        StringBuilder outBuffer = new StringBuilder();
        outBuffer.append("<span class=\"").append(cssClassMap.get(spanNode.getType())).append("\">");
        outBuffer.append(super.renderHtml(node, renderContext).toString());
        outBuffer.append("</span>");
        return outBuffer;
    }
}
