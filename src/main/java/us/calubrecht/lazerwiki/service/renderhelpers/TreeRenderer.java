package us.calubrecht.lazerwiki.service.renderhelpers;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.text.StringEscapeUtils;
import us.calubrecht.lazerwiki.service.RendererRegistrar;

import java.util.ArrayList;
import java.util.List;

public abstract class TreeRenderer {
    protected RendererRegistrar renderers;

    public abstract List<Class<? extends ParseTree>> getTargets();

    public abstract StringBuilder render(ParseTree tree, RenderContext renderContext);

    public abstract StringBuilder renderToPlainText(ParseTree tree, RenderContext renderContext);

    public boolean isAdditive() {
        return false;
    }

    public String getAdditiveClass() {
        return null;
    }

    public boolean shouldParentSanitize() {
        return true;
    }

    public TreeRenderer getSpecificRenderer(ParseTree tree) { return this;}

    public static String sanitize(String input) {
        return StringEscapeUtils.escapeHtml4(input).replaceAll("&quot;", "\"");
    }

    public void setRenderers(RendererRegistrar renderers) {
        this.renderers = renderers;
    }

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

    protected StringBuilder renderChildren(List<ParseTree> trees, RenderContext renderContext) {
        StringBuilder outBuffer = new StringBuilder();
        List<ParseTree> childrenToMerge = new ArrayList<>();
        String lastChildClass = null;
        for(ParseTree child: trees) {
            TreeRenderer renderer = renderers.getRenderer(child.getClass(), child);
            if (lastChildClass != null && !lastChildClass.equals(renderer.getAdditiveClass()) )
            {
                AdditiveTreeRenderer aRenderer = (AdditiveTreeRenderer)renderers.getRenderer(lastChildClass, childrenToMerge.get(0));
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
            AdditiveTreeRenderer aRenderer = (AdditiveTreeRenderer) renderers.getRenderer(lastChildClass, childrenToMerge.get(0));
            outBuffer.append(aRenderer.render(childrenToMerge, renderContext));
        }
        return outBuffer;
    }

    protected StringBuilder renderChildrenToPlainText(List<ParseTree> trees, RenderContext renderContext) {
        StringBuilder outBuffer = new StringBuilder();
        for(ParseTree child: trees) {
            TreeRenderer renderer = renderers.getRenderer(child.getClass(), child);
            outBuffer.append(renderer.renderToPlainText(child, renderContext));
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
        public List<Class<? extends ParseTree>> getTargets() {
            return null;
        }

        @Override
        public StringBuilder render(ParseTree tree, RenderContext renderContext) {
            return renderChildren(getChildren(tree), renderContext);
        }

        @Override
        public StringBuilder renderToPlainText(ParseTree tree, RenderContext renderContext) {
            return renderChildrenToPlainText(getChildren(tree), renderContext);
        }
    }
}
