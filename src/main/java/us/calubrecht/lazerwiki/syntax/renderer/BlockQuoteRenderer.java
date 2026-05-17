package us.calubrecht.lazerwiki.syntax.renderer;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.BlockQuoteNode;
import us.calubrecht.lazerwiki.syntax.nodes.LineBreakNode;
import us.calubrecht.lazerwiki.syntax.nodes.TaggedContainerNode;

import java.util.Collection;
import java.util.List;

@Component
public class BlockQuoteRenderer extends TaggedContainerRenderer {

    @Override
    public Collection<Class<? extends ITreeNode>> getTargets() {
        return List.of(BlockQuoteNode.class);
    }

    @Override
    protected Pair<String,String> getTagNames(TaggedContainerNode.TYPE type) {
        return Pair.of("<blockquote>", "</blockquote>");
    }

    @Override
    public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
        BlockQuoteNode taggedNode = (BlockQuoteNode) node;
        StringBuilder buffer = new StringBuilder();
        for (ITreeNode child : taggedNode.getChildren()) {
            if (!buffer.isEmpty() && child instanceof BlockQuoteNode) {
                buffer.append("\n");
            }
            if (!buffer.isEmpty() && buffer.charAt(buffer.length()-1) =='\n' && child instanceof LineBreakNode) {
                continue; // Avoid excess duplicate linebreaks
            }
            buffer.append(parserRegistrar.getRenderer(child.getClass()).renderPlaintext(child, renderContext));
        }
        return buffer;
    }
}
