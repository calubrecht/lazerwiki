package us.calubrecht.lazerwiki.syntax.renderer;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.TaggedContainerNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class TaggedContainerRenderer extends ContainerRenderer  {
    final Map<TaggedContainerNode.TYPE, Pair<String,String>> tagNameMap = Map.of(
            TaggedContainerNode.TYPE.PARAGRAPH, Pair.of("<div>", "</div>\n"),
            TaggedContainerNode.TYPE.CODE_BLOCK, Pair.of("<pre class=\"code\">", "</pre>")
    );

    @Override
    public Collection<Class<? extends ITreeNode>> getTargets() {
        return List.of(TaggedContainerNode.class);
    }

    protected Pair<String,String> getTagNames(TaggedContainerNode.TYPE type) {
        return tagNameMap.get(type);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        TaggedContainerNode taggedNode = (TaggedContainerNode)node;
        StringBuilder content = super.renderHtml(node, renderContext);
        if (content.isEmpty()) {
            return new StringBuilder();
        }
        StringBuilder buffer = new StringBuilder();
        Pair<String,String> tagNames = getTagNames(taggedNode.getType());
        buffer.append(tagNames.getLeft());
        buffer.append(transformMacro(taggedNode.getType(), content));
        buffer.append(tagNames.getRight());
        return buffer;
    }

    CharSequence transformMacro(TaggedContainerNode.TYPE type, CharSequence builder) {
        if (type == TaggedContainerNode.TYPE.CODE_BLOCK) {
            // Escape Macro characters, prevent rendering macro in postRender
            return builder.toString().replace("~~", "&#126;&#126;");
        }
        return builder;
    }
}
