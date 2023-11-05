package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

@Component
public class TerminalRenderer extends TreeRenderer {

    public Class getTarget() {
        return TerminalNodeImpl.class;
    }

    public StringBuffer render(ParseTree tree) {
        TerminalNodeImpl node = (TerminalNodeImpl)tree;
        return node.getSymbol().getType() == Token.EOF ? new StringBuffer() : new StringBuffer(tree.getText());
    }
}
