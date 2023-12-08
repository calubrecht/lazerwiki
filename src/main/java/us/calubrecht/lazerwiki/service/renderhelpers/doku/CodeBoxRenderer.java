package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.AdditiveTreeRenderer;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.List;

@Component
public class CodeBoxRenderer extends AdditiveTreeRenderer {

    final int indentSpaces = 2;

    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.Code_boxContext.class);
    }

    @Override
    public StringBuilder render(ParseTree tree, RenderContext renderContext) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public StringBuilder renderToPlainText(ParseTree tree, RenderContext renderContext) {
        return new StringBuilder(renderChildrenToPlainText(getChildren(tree), renderContext).toString().trim()).append('\n');
    }

    @Override
    public StringBuilder render(List<ParseTree> trees, RenderContext renderContext) {
        StringBuilder ret = new StringBuilder();
        ret.append("<pre class=\"code\">");
        trees.forEach(t ->
        {
            ret.append(t.getText().substring(indentSpaces));
        });
        ret.append("</pre>");
        return ret;
    }

    @Override
    public String getAdditiveClass() {
        return "Code";
    }
}
