package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.AdditiveTreeRenderer;

import java.util.ArrayList;
import java.util.List;

@Component
public class BlockRenderer extends AdditiveTreeRenderer {
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.BlockContext.class);
    }

    @Override
    public StringBuilder render(List<ParseTree> trees, RenderContext renderContext) {

        List<List<ParseTree>> treeBlocks = new ArrayList<>();
        Class<? extends ParseTree> lastClass = null;
        List<ParseTree> lastTreeBlock = null;
        for (ParseTree block : trees) {
            for (int i = 0; i < block.getChildCount(); i++) {
                ParseTree child = block.getChild(i);
                if (child.getClass() != lastClass) {
                    lastClass = child.getClass();
                    lastTreeBlock = new ArrayList<>();
                    treeBlocks.add(lastTreeBlock);
                }
                lastTreeBlock.add(child);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (List<ParseTree> block: treeBlocks) {
           StringBuilder blockRender = renderChildren(block, renderContext);
           builder.append(blockRender);
        }
        return builder;
    }

    @Override
    public StringBuilder renderToPlainText(ParseTree tree, RenderContext renderContext) {
        return renderChildrenToPlainText(getChildren(tree), renderContext);
    }

    public String getAdditiveClass() {
        return "Block";
    }
}
