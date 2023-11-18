package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.List;

@Component
public class ItalicSpanRenderer  extends TreeRenderer {
    @Override
    public List<Class> getTargets() {
        return List.of(DokuwikiParser.Italic_spanContext.class);
    }

    public StringBuffer render(ParseTree tree) {
        StringBuffer sb = new StringBuffer();
        sb.append("<span class=\"italic\">");
        sb.append(renderChildren(getChildren(tree, 1, tree.getChildCount()-1)));
        sb.append("</span>");
        return sb;
    }
}