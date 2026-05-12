package us.calubrecht.lazerwiki.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.framework.Renderer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
        /*

public record RenderResult (String renderedText, String plainText, Map<String, Object> renderState)
         */
        ITreeNode node = parser.parse(markup);
        Map<String, Object> renderState = new HashMap<>();
        String rendered = renderer.render(node, renderState);
        return new RenderResult(rendered, "", renderState);
    }

    @Override
    public String renderToString(String markup, RenderContext renderContext) {
        return renderWithInfo(markup, renderContext).renderedText();
       /* Parser parser = Parser.builder()
                .enabledBlockTypes(Collections.emptySet()) // Disable markdown parsers
                .customBlockParserFactory(new HeaderParser.Factory())
                .build();
        HtmlRenderer renderer = HtmlRenderer.builder()
                .nodeRendererFactory((context) -> {
                    return new HeaderRenderer(context);
                })
                .build();
        Node n = parser.parse(markup);
        return renderer.render(n);*/
    }

    @Override
    public String renderToPlainText(String markup, RenderContext renderContext) {
        return renderWithInfo(markup, renderContext).plainText();
    }
}
