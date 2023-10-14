package us.calubrecht.lazerwiki.service.helpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.helpers.TreeRenderer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

@Component
public class HeaderRenderer extends SerialRenderer {
    public Class getTarget() {
        return DokuwikiParser.HeaderContext.class;
    }

    public StringBuffer render(ParseTree tree) {
        DokuwikiParser.HeaderContext context = (DokuwikiParser.HeaderContext)tree;
        context.getChild(0).getText();
        int headerSize = context.getChild(0).getText().length();
        String hTag = "h" + (7 - headerSize);
        StringBuffer outBuffer = new StringBuffer();
        outBuffer.append("<").append(hTag).append(">");
        outBuffer.append(renderChildren(tree, 1, tree.getChildCount()-1).toString().strip());
        outBuffer.append("</").append(hTag).append(">");
        return outBuffer;

    }
}
