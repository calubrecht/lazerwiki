package us.calubrecht.lazerwiki.syntax.renderer;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.model.HeaderRef;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.BoldNode;
import us.calubrecht.lazerwiki.syntax.nodes.HeaderNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.HEADERS;
import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.ID_SUFFIX;

@Component
public class BoldRenderer extends ContainerRenderer {
    final String cssClass = "bold";
    @Override
    public Collection<Class> getTargets() {
        return List.of(BoldNode.class);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        StringBuilder outBuffer = new StringBuilder();
        outBuffer.append("<span class=\"").append(cssClass).append("\">");
        outBuffer.append(super.renderHtml(node, renderContext).toString());
        outBuffer.append("</span>");
        return outBuffer;
    }
}
