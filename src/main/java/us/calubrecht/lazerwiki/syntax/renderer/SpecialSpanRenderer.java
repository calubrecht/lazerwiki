package us.calubrecht.lazerwiki.syntax.renderer;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.SpecialSpanNode;
import us.calubrecht.lazerwiki.syntax.nodes.SpecialSpanNode.SPAN_TYPE;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class SpecialSpanRenderer extends ContainerRenderer {
    final Map<SpecialSpanNode.SPAN_TYPE, String> tagNameMap =
            Map.of(SPAN_TYPE.SUP, "sup",
                SPAN_TYPE.SUB, "sub",
                SPAN_TYPE.DEL, "del");
    @Override
    public Collection<Class<? extends ITreeNode>> getTargets() {
        return List.of(SpecialSpanNode.class);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        SpecialSpanNode spanNode = (SpecialSpanNode)node;
        StringBuilder outBuffer = new StringBuilder();
        String tagName = tagNameMap.get(spanNode.getType());
        outBuffer.append(String.format("<%s>", tagName));
        outBuffer.append(super.renderHtml(node, renderContext).toString());
        outBuffer.append(String.format("</%s>", tagName));
        return outBuffer;
    }
}
