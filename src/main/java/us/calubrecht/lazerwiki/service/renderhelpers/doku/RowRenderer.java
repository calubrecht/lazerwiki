package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class RowRenderer extends FlatteningRenderer {

    final Set<Class<? extends ParseTree>> treesToFlatten = Set.of(DokuwikiParser.LineContext.class, DokuwikiParser.Line_itemContext.class);

    @Autowired
    TableRenderer tableRenderer;

    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.RowContext.class);
    }

    @Override
    Set<Class<? extends ParseTree>> getTreesToFlatten() {
        return treesToFlatten;
    }

    @Override
    public StringBuilder renderToPlainText(ParseTree tree, RenderContext renderContext) {
        return renderChildrenToPlainText(getChildren(tree), renderContext);
    }

    protected String getTagName() {
        return "div";
    }

    @Override
    public StringBuilder render(List<ParseTree> trees, RenderContext renderContext) {
        StringBuilder ret = new StringBuilder();
        ret.append("<%s>".formatted(getTagName()));
        List<ParseTree> children = trees.stream().flatMap(
                (t) -> flattenChildren(t, false).stream()).collect(Collectors.toList());
        ret.append(renderChildren(children, renderContext));
        // Remove trailing new line
        if (ret.charAt(ret.length() -1) == '\n') {
            ret.deleteCharAt(ret.length() -1);
        }
        ret.append("</%s>".formatted(getTagName()));
        return ret;
    }

    @Override
    public String getAdditiveClass() {
        return "Row";
    }

    static final Pattern tablePattern = Pattern.compile("[|^].*[|^]");
    @Override
    public TreeRenderer getSpecificRenderer(ParseTree tree) {
        if (tree == null)  {
            return this;
        }
        // Get text and strip trailing newline
        String text = tree.getText().substring(0, tree.getText().length()-1);
        if (tablePattern.matcher(text).matches()){
            return tableRenderer;
        }
        return this;
    }
}
