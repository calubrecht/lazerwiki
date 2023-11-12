package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

@Component
public class BoldSpanRenderer extends TreeRenderer {
    public Class getTarget() {
        return DokuwikiParser.Bold_spanContext.class;
    }

    public StringBuffer render(ParseTree tree) {
        StringBuffer sb = new StringBuffer();
        sb.append("<span class=\"bold\">");
        sb.append(renderChildren(tree, 1, tree.getChildCount()-1));
        sb.append("</span>");
        return sb;
    }
}
