package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.AdditiveTreeRenderer;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RowRenderer extends AdditiveTreeRenderer {

    Set<Class> treesToFlatten = Set.of(DokuwikiParser.LineContext.class);

    @Override
    public Class getTarget() {
        return DokuwikiParser.RowContext.class;
    }

    @Override
    public StringBuffer render(ParseTree tree) {
        StringBuffer outBuffer = renderChildren(getChildren(tree, 0, getChildCount(tree)));
        return new StringBuffer(outBuffer.toString().trim());
    }

    @Override
    public StringBuffer render(List<ParseTree> trees) {
        StringBuffer ret = new StringBuffer();
        ret.append("<div>");
        List<ParseTree> children = trees.stream().flatMap(
                (t) -> flattenChildren(t).stream()).collect(Collectors.toList());
        ret.append(renderChildren(children));
        if (ret.charAt(ret.length() -1) == '\n') {
            ret.deleteCharAt(ret.length() -1);
        }
        ret.append("</div>");
        return ret;
    }
    
    @Override
    public boolean isAdditive() {
        return true;
    }

    List<ParseTree> flattenChildren(ParseTree tree) {
        List<ParseTree> trees = new ArrayList<>();
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree t = tree.getChild(i);
            if (treesToFlatten.contains(t.getClass())){
                for (int j = 0; j < t.getChildCount(); j++) {
                    ParseTree child = t.getChild(j);
                    if (!isEOL(child)) {
                        trees.add(child);
                    }
                }
            }
            else {
                if (!isEOL(t)) {
                    trees.add(t);
                }
            }
        }
        return trees;
    }

    protected ParseTree getChild(ParseTree tree, int index) {
        return flattenChildren(tree).get(index);
    }

    protected int getChildCount(ParseTree tree) {
        return flattenChildren(tree).size();
    }
}
