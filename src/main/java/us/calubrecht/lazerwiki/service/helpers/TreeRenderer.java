package us.calubrecht.lazerwiki.service.helpers;

import org.antlr.v4.runtime.tree.ParseTree;
import us.calubrecht.lazerwiki.service.RendererRegistrar;

public abstract class TreeRenderer {
    protected RendererRegistrar renderers;

    public abstract Class getTarget();

    public abstract StringBuffer render(ParseTree tree);

    public void setRenderers(RendererRegistrar renderers) {
        this.renderers = renderers;
    }

    public static TreeRenderer DEFAULT = new DefaultRenderer();
    public static class DefaultRenderer extends TreeRenderer {
        public Class getTarget() {
            return null;
        }

        public StringBuffer render(ParseTree tree) {
            return new StringBuffer(tree.getText());
        }
    }
}
