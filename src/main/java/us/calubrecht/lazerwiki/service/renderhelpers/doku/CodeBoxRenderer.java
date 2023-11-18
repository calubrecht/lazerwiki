package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.AdditiveTreeRenderer;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CodeBoxRenderer extends AdditiveTreeRenderer {

    int indentSpaces = 2;

    @Override
    public List<Class> getTargets() {
        return List.of(DokuwikiParser.Code_boxContext.class);
    }

    @Override
    public StringBuffer render(ParseTree tree) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public StringBuffer render(List<ParseTree> trees) {
        StringBuffer ret = new StringBuffer();
        ret.append("<pre class=\"code\">");
        trees.forEach(t ->
        {
            ret.append(t.getText().substring(indentSpaces));
        ;});
        ret.append("</pre>");
        return ret;
    }

    @Override
    public String getAdditiveClass() {
        return "Code";
    }
}