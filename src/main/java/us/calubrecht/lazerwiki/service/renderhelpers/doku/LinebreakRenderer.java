package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.List;

@Component
public class LinebreakRenderer extends TreeRenderer {

    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.Line_breakContext.class);
    }

    @Override
    public StringBuffer render(ParseTree tree, RenderContext renderContext) {
        return new StringBuffer("<br>");
    }

    @Override
    public StringBuffer renderToPlainText(ParseTree tree, RenderContext renderContext) {
        return new StringBuffer("\n");
    }
}
