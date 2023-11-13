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
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public StringBuffer render(List<ParseTree> trees) {
        StringBuffer ret = new StringBuffer();
        ret.append("<div>");
        List<ParseTree> children = trees.stream().flatMap(
                (t) -> flattenChildren(t).stream()).collect(Collectors.toList());
        ret.append(renderChildren(children));
        // Remove trailing new line
        ret.deleteCharAt(ret.length() -1);
        ret.append("</div>");
        return ret;
    }

    @Override
    public String getAdditiveClass() {
        return "Row";
    }

    @Override
    public boolean isAdditive() {
        return true;
    }

    List<ParseTree> flattenChildren(ParseTree tree) {
        List<ParseTree> trees = new ArrayList<>();
        TreeRenderer lastRenderer = null;
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree t = tree.getChild(i);
            if (treesToFlatten.contains(t.getClass())){
                for (int j = 0; j < t.getChildCount(); j++) {
                    ParseTree child = t.getChild(j);
                    trees.add(child);
                    lastRenderer = renderers.getRenderer(child.getClass());
                }
            }
            else {
                if (isEOL(t)) {
                    if (lastRenderer.isAdditive()) {
                        continue;
                    }
                }
                trees.add(t);
                lastRenderer = renderers.getRenderer(t.getClass());
            }
        }
        return trees;
    }
}
