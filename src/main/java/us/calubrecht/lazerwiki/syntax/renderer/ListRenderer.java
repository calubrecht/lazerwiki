package us.calubrecht.lazerwiki.syntax.renderer;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.ListItemNode;
import us.calubrecht.lazerwiki.syntax.nodes.ListNode;
import us.calubrecht.lazerwiki.syntax.nodes.ListNode.LIST_TYPE;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class ListRenderer extends AbstractRenderer {
    Map<LIST_TYPE, String> tagNameMap = Map.of(
            LIST_TYPE.ORDERED, "ol",
            LIST_TYPE.UNORDERED, "ul");

    ListItemRenderer listItemRenderer = new ListItemRenderer();

    @Override
    public Collection<Class> getTargets() {
        return List.of(ListNode.class);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        ListNode listNode = (ListNode) node;
        String tagName = tagNameMap.get(listNode.getListType());
        StringBuilder buffer = new StringBuilder();
        buffer.append(String.format("<%s>\n", tagName));
        buffer.append(renderChildren(listNode.getItems(), renderContext));
        buffer.append(String.format("</%s>\n", tagName));
        return buffer;
    }

    @Override
    public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
        return null;
    }

    StringBuilder renderChildren(List<ListItemNode> items, RenderContext renderContext) {
        listItemRenderer.setRegistrar(parserRegistrar);
        StringBuilder buffer = new StringBuilder();
        for (ListItemNode item : items) {
            buffer.append(listItemRenderer.renderHtml(item, renderContext));
            buffer.append("\n");
        }
        return buffer;
    }
    public static class ListItemRenderer extends ContainerRenderer {
        @Override
        public Collection<Class> getTargets() {
            return List.of(ListItemNode.class);
        }

        @Override
        public StringBuilder renderHtml(ITreeNode node, RenderContext context) {
            StringBuilder builder = new StringBuilder();
            builder.append("<li>");
            builder.append(super.renderHtml(node, context).toString().strip());
            builder.append("</li>");
            return builder;
        }
    }
}
