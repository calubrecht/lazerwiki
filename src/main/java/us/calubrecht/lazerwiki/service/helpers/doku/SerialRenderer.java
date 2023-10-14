package us.calubrecht.lazerwiki.service.helpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import us.calubrecht.lazerwiki.service.helpers.TreeRenderer;

public abstract class SerialRenderer extends TreeRenderer {

    protected StringBuffer renderChildren(ParseTree tree, int firstChild, int lastChild) {
        StringBuffer outBuffer = new StringBuffer();
        for(int i = firstChild; i < lastChild; i++) {
            ParseTree child = tree.getChild(i);
            outBuffer.append(renderers.getRenderer(child.getClass()).render(child));
        }
        return outBuffer;
    }
}
