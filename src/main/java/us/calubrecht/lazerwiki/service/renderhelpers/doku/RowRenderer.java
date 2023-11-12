package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RowRenderer extends TreeRenderer {

    public Class getTarget() {
        return DokuwikiParser.RowContext.class;
    }

    public StringBuffer render(ParseTree tree) {
        StringBuffer outBuffer = renderChildren(tree, 0, tree.getChildCount());
        return new StringBuffer(outBuffer.toString().trim());
    }

    public StringBuffer render(List<ParseTree> trees) {
        StringBuffer ret = new StringBuffer();
        ret.append("<div>");
        ret.append(
                trees.stream().map(t -> render(t)).collect(Collectors.joining("\n")));
        ret.append("</div>");
        return ret;
    }
    public boolean isAdditive() {
        return true;
    }
}
