package us.calubrecht.lazerwiki.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.framework.Renderer;

/**
 * An implementation of IMarkupRenderer that speaks DokuWiki's markup language.
 */
@Service
@Qualifier("New")
public class CustomWikiRenderer implements IMarkupRenderer {
    final Parser parser;
    final Renderer renderer;

    public CustomWikiRenderer(@Autowired Parser parser, @Autowired Renderer renderer) {
        this.parser = parser;
        this.renderer = renderer;
    }

    @Override
    public RenderResult renderWithInfo(String markup, RenderContext renderContext) {
        ITreeNode node = parser.parse(markup);
        String rendered = renderer.render(node, renderContext);
        String plainText = renderer.renderPlaintext(node, renderContext);
        return new RenderResult(rendered, plainText, renderContext.renderState());
    }

    @Override
    public String renderToString(String markup, RenderContext renderContext) {
        return renderWithInfo(markup, renderContext).renderedText();
    }

    @Override
    public String renderToPlainText(String markup, RenderContext renderContext) {
        return renderWithInfo(markup, renderContext).plainText();
    }
}
