package us.calubrecht.lazerwiki.service.renderhelpers;

import org.antlr.v4.runtime.tree.ParseTree;
import us.calubrecht.lazerwiki.service.RendererRegistrar;

import java.util.List;
import java.util.stream.Collectors;

public abstract class TreeRenderer {
    protected RendererRegistrar renderers;

    public abstract Class getTarget();

    public abstract StringBuffer render(ParseTree tree);

    public StringBuffer render(List<ParseTree> trees) {
        StringBuffer ret = new StringBuffer();
        ret.append(
         trees.stream().map(t -> render(t)).collect(Collectors.joining()));
        return ret;
    }

    public boolean isAdditive() {
        return false;
    }

    public boolean shouldParentSanitize() {
        return true;
    }

    public void setRenderers(RendererRegistrar renderers) {
        this.renderers = renderers;
    }

    public static TreeRenderer DEFAULT = new DefaultRenderer();

    protected StringBuffer renderChildren(ParseTree tree, int firstChild, int lastChild) {
        StringBuffer outBuffer = new StringBuffer();
        for(int i = firstChild; i < lastChild; i++) {
            ParseTree child = tree.getChild(i);
            outBuffer.append(renderers.getRenderer(child.getClass()).render(child));
        }
        return outBuffer;
    }

    public static class DefaultRenderer extends TreeRenderer {
        public Class getTarget() {
            return null;
        }

        public StringBuffer render(ParseTree tree) {
            return renderChildren(tree, 0, tree.getChildCount());
        }
    }
}
