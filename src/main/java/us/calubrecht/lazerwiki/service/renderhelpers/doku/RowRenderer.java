package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

@Component
public class RowRenderer extends TreeRenderer {

    public Class getTarget() {
        return DokuwikiParser.RowContext.class;
    }

    public StringBuffer render(ParseTree tree) {
        StringBuffer outBuffer = renderChildren(tree, 0, tree.getChildCount());
        return outBuffer;

    }
}
