package us.calubrecht.lazerwiki.syntax.renderer;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.ListChild;
import us.calubrecht.lazerwiki.syntax.nodes.ListNode;
import us.calubrecht.lazerwiki.syntax.nodes.ListNode.LIST_TYPE;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component("customSynListRenderer")
public class ListRenderer extends AbstractRenderer {
    final Map<LIST_TYPE, String> tagNameMap = Map.of(
            LIST_TYPE.ORDERED, "ol",
            LIST_TYPE.UNORDERED, "ul");

    final ListItemRenderer listItemRenderer = new ListItemRenderer();

    @Override
    public Collection<Class<? extends ITreeNode>> getTargets() {
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

    StringBuilder renderChildren(List<ListChild> items, RenderContext renderContext) {
        listItemRenderer.setRegistrar(parserRegistrar);
        StringBuilder buffer = new StringBuilder();
        for (ListChild item : items) {
            if (item instanceof ListChild.ListChildList listNode) {
                buffer.append(renderHtml(listNode.list(), renderContext));
                continue;
            }
            ListChild.ListItemNode itemNode = (ListChild.ListItemNode)item;
            buffer.append(listItemRenderer.renderHtml(itemNode, renderContext));
            buffer.append("\n");
        }
        return buffer;
    }
    public static class ListItemRenderer extends ContainerRenderer {
        @Override
        public Collection<Class<? extends ITreeNode>> getTargets() {
            return List.of(ListChild.ListItemNode.class);
        }

        @Override
        public StringBuilder renderHtml(ITreeNode node, RenderContext context) {
            ListChild.ListItemNode itemNode = (ListChild.ListItemNode)node;
            StringBuilder builder = new StringBuilder();
            String value = itemNode.getValue() != null ? String.format(" value=\"%s\"", itemNode.getValue()) : "";
            builder.append(String.format("<li%s>", value));
            builder.append(super.renderHtml(node, context).toString().strip());
            builder.append("</li>");
            return builder;
        }
    }
}
