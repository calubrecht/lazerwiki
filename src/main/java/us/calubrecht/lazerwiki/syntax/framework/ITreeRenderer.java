package us.calubrecht.lazerwiki.syntax.framework;

import org.apache.commons.text.StringEscapeUtils;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.Collection;
import java.util.List;

public interface ITreeRenderer {
    Collection<Class<? extends ITreeNode>> getTargets();

    void setRegistrar(ParserRegistrar registrar);

    StringBuilder renderHtml(ITreeNode node, RenderContext renderContext);
    StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext);

    class DefaultRenderer implements ITreeRenderer {

        @Override
        public Collection<Class<? extends ITreeNode>> getTargets() {
            return List.of();
        }

        @Override
        public void setRegistrar(ParserRegistrar registrar) {

        }

        @Override
        public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
            return new StringBuilder(sanitize(node.asString()));
        }

        @Override
        public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
            return new StringBuilder(node.asString());
        }
    }

    default String sanitize(String input) {
        return StringEscapeUtils.escapeHtml4(input).replace("&quot;", "\"");
    }
}
