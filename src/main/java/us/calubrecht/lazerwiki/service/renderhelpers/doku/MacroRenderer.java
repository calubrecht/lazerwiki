package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.MacroService;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.List;

@Component
public class MacroRenderer extends TreeRenderer {

    @Autowired
    MacroService macroService;

    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.MacroContext.class);
    }

    @Override
    public StringBuilder render(ParseTree tree, RenderContext context) {
        String treeText = tree.getText();
        String innerText = treeText.substring("~~MACRO~~".length(), treeText.length() - "~~/MACRO~~".length());
        return new StringBuilder(macroService.renderMacro(innerText, treeText, context));
    }

    @Override
    public StringBuilder renderToPlainText(ParseTree tree, RenderContext renderContext) {
        return new StringBuilder();
    }
}
