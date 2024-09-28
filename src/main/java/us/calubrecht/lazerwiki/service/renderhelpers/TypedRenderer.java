package us.calubrecht.lazerwiki.service.renderhelpers;

import org.antlr.v4.runtime.tree.ParseTree;

public abstract class TypedRenderer<T extends ParseTree> extends TreeRenderer {
    @SuppressWarnings("unchecked")
    public T toContext(ParseTree tree) { return (T)tree;}

    public abstract StringBuilder renderContext(T context, RenderContext renderContext);
    public abstract StringBuilder renderContextToPlainText(T context, RenderContext renderContext);

    @Override
    public StringBuilder render(ParseTree tree, RenderContext renderContext) {
        return renderContext(toContext(tree), renderContext);
    }

    @Override
    public StringBuilder renderToPlainText(ParseTree tree, RenderContext renderContext) {
        return renderContextToPlainText(toContext(tree), renderContext);
    }
}
