package us.calubrecht.lazerwiki.service.renderhelpers;

import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

/**
 * A tree renderer for a ParseTree class that is intended to merge with
 * other adjacent ParseTrees of the same class
 */
public abstract class AdditiveTreeRenderer extends TreeRenderer {

    public abstract StringBuffer render(List<ParseTree> trees, RenderContext renderContext);

    @Override
    public StringBuffer render(ParseTree tree, RenderContext renderContext) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public boolean isAdditive() {
        return true;
    }
}
