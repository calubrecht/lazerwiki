package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

public abstract class AbstractSpanRenderer extends TreeRenderer {
    final String startTag;
    final String endTag;

    public AbstractSpanRenderer(String startTag, String endTag) {
        this.startTag = startTag;
        this.endTag = endTag;
    }

    public StringBuffer render(ParseTree tree, RenderContext renderContext) {
        StringBuffer sb = new StringBuffer();
        sb.append(startTag);
        sb.append(renderChildren(getChildren(tree, 1, tree.getChildCount()-1), renderContext));
        sb.append(endTag);
        return sb;
    }

    @Override
    public StringBuffer renderToPlainText(ParseTree tree, RenderContext renderContext) {
        StringBuffer sb = new StringBuffer();
        sb.append(renderChildrenToPlainText(getChildren(tree, 1, tree.getChildCount()-1), renderContext));
        return sb;
    }

}
