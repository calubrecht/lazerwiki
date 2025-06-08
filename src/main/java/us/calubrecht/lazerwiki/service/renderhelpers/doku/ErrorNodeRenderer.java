package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ErrorNodeImpl;
import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.AdditiveTreeRenderer;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ErrorNodeRenderer extends AdditiveTreeRenderer {

    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(ErrorNodeImpl.class);
    }

    @Override
    public StringBuilder render(List<ParseTree> trees, RenderContext renderContext) {
        String value = sanitize(trees.stream().map(ParseTree::getText).collect(Collectors.joining())).
                replace("\n", "<br>");
        return new StringBuilder(String.format("<div class=\"parseError\"><b>ERROR:</b> Cannot parse: [%s]</div>", value));
    }

    @Override
    public StringBuilder renderToPlainText(ParseTree tree, RenderContext renderContext) {
        return new StringBuilder(String.format("ERROR: Cannot parse: [%s]\n", tree.getText()));
    }

    @Override
    public String getAdditiveClass() {
        return "Error";
    }
}
