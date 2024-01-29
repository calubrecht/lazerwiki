package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.List;

@Component
public class UnformatRenderer extends TreeRenderer {
    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.Unformat_spanContext.class);
    }

    @Override
    public StringBuilder render(ParseTree tree, RenderContext renderContext) {
        StringBuilder sb = new StringBuilder();
        List<ParseTree> children = getChildren(tree, 1, tree.getChildCount()-1);
        for (ParseTree child: children) {
            sb.append(child.getText());
        }
        return sb;
    }

    @Override
    public StringBuilder renderToPlainText(ParseTree tree, RenderContext renderContext) {
        return render(tree, renderContext);
    }
}
