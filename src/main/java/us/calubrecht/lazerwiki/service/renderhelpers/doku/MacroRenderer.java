package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.MacroService;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MacroRenderer extends TreeRenderer {

    @Autowired
    MacroService macroService;

    @Override
    public List<Class> getTargets() {
        return List.of(DokuwikiParser.MacroContext.class);
    }

    Pattern macroPattern= Pattern.compile("~~MACRO~~(.*)~~/MACRO~~");

    @Override
    public StringBuffer render(ParseTree tree, RenderContext context) {
        Matcher matcher = macroPattern.matcher(tree.getText());
        matcher.matches();
        String innerText = matcher.group(1);
        return new StringBuffer(macroService.renderMacro(innerText, context));
    }
}
