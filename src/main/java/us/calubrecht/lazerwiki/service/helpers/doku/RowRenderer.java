package us.calubrecht.lazerwiki.service.helpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.RendererRegistrar;
import us.calubrecht.lazerwiki.service.helpers.TreeRenderer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

@Component
public class RowRenderer extends SerialRenderer {

    public Class getTarget() {
        return DokuwikiParser.RowContext.class;
    }

    public StringBuffer render(ParseTree tree) {
        StringBuffer outBuffer = renderChildren(tree, 0, tree.getChildCount());
        return outBuffer;

    }
}
