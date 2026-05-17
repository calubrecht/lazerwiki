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
            TaggedContainerNode.TYPE.CODE_BLOCK, Pair.of("<pre class=\"code\">", "</pre>"),
            TaggedContainerNode.TYPE.BLOCK_QUOTE, Pair.of("<blockquote>", "</blockquote>")
    );

    @Override
    public Collection<Class<? extends ITreeNode>> getTargets() {
        return List.of(TaggedContainerNode.class);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        TaggedContainerNode taggedNode = (TaggedContainerNode)node;
        StringBuilder content = super.renderHtml(node, renderContext);
        if (content.isEmpty()) {
            return new StringBuilder();
        }
        StringBuilder buffer = new StringBuilder();
        Pair<String,String> tagNames = tagNameMap.get(taggedNode.getType());
        buffer.append(tagNames.getLeft());
        buffer.append(content);
        buffer.append(tagNames.getRight());
        return buffer;
    }
}
