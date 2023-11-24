package us.calubrecht.lazerwiki.service.renderhelpers;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import us.calubrecht.lazerwiki.service.RendererRegistrar;

import java.util.ArrayList;
import java.util.List;

public abstract class TreeRenderer {
    protected RendererRegistrar renderers;

    public abstract List<Class> getTargets();

    public abstract StringBuffer render(ParseTree tree, RenderContext context);

    public boolean isAdditive() {
        return false;
    }

    public String getAdditiveClass() {
        return null;
    }

    public boolean shouldParentSanitize() {
        return true;
    }

    public void setRenderers(RendererRegistrar renderers) {
        this.renderers = renderers;
    }

    public static TreeRenderer DEFAULT = new DefaultRenderer();

    protected List<ParseTree> getChildren(ParseTree tree) {
        return getChildren(tree, 0, tree.getChildCount());
    }

    protected List<ParseTree> getChildren(ParseTree tree, int start, int end) {
        List<ParseTree> children = new ArrayList<>();
        for (int i = start; i < end; i++) {
            children.add(tree.getChild(i));
        }
        return children;
    }

    protected StringBuffer renderChildren(List<ParseTree> trees, RenderContext renderContext) {
        StringBuffer outBuffer = new StringBuffer();
        List<ParseTree> childrenToMerge = new ArrayList<>();
        String lastChildClass = null;
        for(ParseTree child: trees) {
            TreeRenderer renderer = renderers.getRenderer(child.getClass());
            if (lastChildClass != null && lastChildClass != renderer.getAdditiveClass() )
            {
                AdditiveTreeRenderer aRenderer = (AdditiveTreeRenderer)renderers.getRenderer(lastChildClass);
                outBuffer.append(aRenderer.render(childrenToMerge, renderContext));
                lastChildClass = null;
                childrenToMerge.clear();
            }
            if (renderer.isAdditive()) {
                lastChildClass = renderer.getAdditiveClass();
                childrenToMerge.add(child);
                continue;
            }
            outBuffer.append(renderer.render(child, renderContext));
        }
        if (lastChildClass != null) {
            AdditiveTreeRenderer aRenderer = (AdditiveTreeRenderer) renderers.getRenderer(lastChildClass);
            outBuffer.append(aRenderer.render(childrenToMerge, renderContext));
        }
        return outBuffer;
    }

    /*
    Unused
    protected boolean isEOL(ParseTree tree) {
        return tree.getText().equals("\n");
    }*/

    public static class DefaultRenderer extends TreeRenderer {
        @Override
        public List<Class> getTargets() {
            return null;
        }

        @Override
        public StringBuffer render(ParseTree tree, RenderContext renderContext) {
            return renderChildren(getChildren(tree), renderContext);
        }
    }
}
