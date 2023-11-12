package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.AdditiveTreeRenderer;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RowRenderer extends AdditiveTreeRenderer {

    @Override
    public Class getTarget() {
        return DokuwikiParser.RowContext.class;
    }

    @Override
    public StringBuffer render(ParseTree tree) {
        StringBuffer outBuffer = renderChildren(tree, 0, tree.getChildCount());
        return new StringBuffer(outBuffer.toString().trim());
    }

    @Override
    public StringBuffer render(List<ParseTree> trees) {
        StringBuffer ret = new StringBuffer();
        ret.append("<div>");
        ret.append(
                trees.stream().map(t -> render(t)).collect(Collectors.joining("\n")));
        ret.append("</div>");
        return ret;
    }
    
    @Override
    public boolean isAdditive() {
        return true;
    }
}
