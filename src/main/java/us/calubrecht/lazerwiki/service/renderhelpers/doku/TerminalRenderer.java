package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.List;

@Component
public class TerminalRenderer extends TreeRenderer {

    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(TerminalNodeImpl.class);
    }

    @Override
    public StringBuffer render(ParseTree tree, RenderContext renderContext) {
        TerminalNodeImpl node = (TerminalNodeImpl)tree;
        return node.getSymbol().getType() == Token.EOF ? new StringBuffer() : new StringBuffer(tree.getText());
    }

    @Override
    public StringBuffer renderToPlainText(ParseTree tree, RenderContext renderContext) {
        return render(tree, renderContext);
    }
}
