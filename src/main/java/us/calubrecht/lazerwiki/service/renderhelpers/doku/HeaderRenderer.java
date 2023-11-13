package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

import java.util.ArrayList;
import java.util.List;

@Component
public class HeaderRenderer extends TreeRenderer {
    public Class getTarget() {
        return DokuwikiParser.HeaderContext.class;
    }

    protected List<ParseTree> getChildren(ParseTree tree) {
        List<ParseTree> children = new ArrayList<>();
        boolean inHeader = false;
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            if (child.getClass() == DokuwikiParser.Header_tokContext.class) {
                if (inHeader){
                    break;
                }
                inHeader = true;
                continue;
            }
            if (inHeader) {
                children.add(child);
            }
        }
        return children;
    }

    public StringBuffer render(ParseTree tree) {
        DokuwikiParser.HeaderContext context = (DokuwikiParser.HeaderContext)tree;
        int headerSize = context.getChild(0).getText().length();
        String hTag = "h" + (7 - headerSize);
        StringBuffer outBuffer = new StringBuffer();
        outBuffer.append("<").append(hTag).append(">");
        outBuffer.append(renderChildren(getChildren(tree)).toString().strip());
        outBuffer.append("</").append(hTag).append(">\n");
        return outBuffer;

    }
}
