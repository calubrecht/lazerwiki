package us.calubrecht.lazerwiki.syntax.framework;

import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.Collection;
import java.util.List;

public interface ITreeRenderer {
    Collection<Class> getTargets();

    void setRegistrar(ParserRegistrar registrar);

    StringBuilder renderHtml(ITreeNode node, RenderContext renderContext);
    StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext);

    public static class DefaultRenderer implements ITreeRenderer {

        @Override
        public Collection<Class> getTargets() {
            return List.of();
        }

        @Override
        public void setRegistrar(ParserRegistrar registrar) {

        }

        @Override
        public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
            return new StringBuilder(node.asString());
        }

        @Override
        public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
            return new StringBuilder(node.asString());
        }
    }
}
