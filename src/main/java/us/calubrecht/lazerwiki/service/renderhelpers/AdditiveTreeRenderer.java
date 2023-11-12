package us.calubrecht.lazerwiki.service.renderhelpers;

import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

/**
 * A tree renderer for a ParseTree class that is intended to merge with
 * other adjacent ParseTrees of the same class
 */
public abstract class AdditiveTreeRenderer extends TreeRenderer {

    public abstract StringBuffer render(List<ParseTree> trees);
}
