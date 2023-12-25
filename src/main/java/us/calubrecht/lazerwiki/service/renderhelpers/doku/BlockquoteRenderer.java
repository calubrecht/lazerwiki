package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.AdditiveTreeRenderer;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class BlockquoteRenderer extends AdditiveTreeRenderer {
    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.BlockquoteContext.class);
    }

    @Override
    public StringBuilder renderToPlainText(ParseTree tree, RenderContext renderContext) {
        return renderChildrenToPlainText(getChildren(tree), renderContext);
    }

    @Override
    public String getAdditiveClass() {
        return "Blockquote";
    }
    @Override
    protected List<ParseTree> getChildren(ParseTree tree) {
        List<ParseTree> allChildren = getChildren(tree, 0, tree.getChildCount());
        List<ParseTree> returnChildren = new ArrayList<>(allChildren);
        while(returnChildren.get(0).getText().equals(">")) {
            returnChildren.remove(0);
        }
        return returnChildren;
    }

    int getLevel(ParseTree tree) {
        int i = 0;
        List<ParseTree> allChildren = getChildren(tree, 0, tree.getChildCount());
        while (allChildren.get(i).getText().equals(">")) {
            i++;
        }
        return i;
    }

    StringBuilder renderBlockquote(List<ParseTree> trees, RenderContext renderContext, int level) {
        StringBuilder ret = new StringBuilder();
        ret.append("<blockquote>");
          ParseTree nextTree = trees.get(0);
        boolean firstLine = true;
        while (getLevel(nextTree) >= level) {
            if (!firstLine) {
                ret.append("<br>");
            }
            if (getLevel(nextTree) > level) {
                ret.append(renderBlockquote(trees, renderContext,level+1));
                firstLine = false;
            }
            else {
                trees.remove(0);
                ret.append(renderChildren(getChildren(nextTree), renderContext));
                firstLine = false;
            }
            if (trees.size() == 0) {
                break;
            }
            nextTree = trees.get(0);
        }
        ret.append("</blockquote>");
        return ret;
    }

    @Override
    public StringBuilder render(List<ParseTree> trees, RenderContext renderContext) {
        return renderBlockquote(new ArrayList<>(trees), renderContext, 1);
    }
}
