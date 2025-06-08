package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class HorizontalRuleRenderer extends TreeRenderer {
    Pattern p = Pattern.compile("----+");

    @Autowired
    ErrorNodeRenderer errorRenderer;

    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.Horizontal_ruleContext.class);
    }

    @Override
    public StringBuilder render(ParseTree tree, RenderContext renderContext) {
        if (isInvalid(tree)) {
            return errorRenderer.render(List.of(tree), renderContext);
        }
        return new StringBuilder("<hr>");
    }

    @Override
    public StringBuilder renderToPlainText(ParseTree tree, RenderContext renderContext) {
        if (isInvalid(tree)) {
            return errorRenderer.renderToPlainText(tree, renderContext);
        }
        return new StringBuilder(tree.getText());
    }

    boolean isInvalid(ParseTree tree) {
        return !p.matcher(tree.getText()).matches();
    }
}
