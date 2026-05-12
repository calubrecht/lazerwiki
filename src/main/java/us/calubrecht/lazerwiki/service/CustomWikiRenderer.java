package us.calubrecht.lazerwiki.service;

import org.commonmark.internal.DocumentParser;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.custommark.HeaderParser;
import us.calubrecht.lazerwiki.service.custommark.HeaderRenderer;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.Collections;

/**
 * An implementation of IMarkupRenderer that speaks DokuWiki's markup language.
 */
@Service
@Qualifier("New")
public class CustomWikiRenderer implements IMarkupRenderer {
    @Override
    public RenderResult renderWithInfo(String markup, RenderContext renderContext) {
        return null;
    }

    @Override
    public String renderToString(String markup, RenderContext renderContext) {
        Parser parser = Parser.builder()
                .enabledBlockTypes(Collections.emptySet()) // Disable markdown parsers
                .customBlockParserFactory(new HeaderParser.Factory())
                .build();
        HtmlRenderer renderer = HtmlRenderer.builder()
                .nodeRendererFactory((context) -> {
                    return new HeaderRenderer(context);
                })
                .build();
        Node n = parser.parse(markup);
        return renderer.render(n);
    }

    @Override
    public String renderToPlainText(String markup, RenderContext renderContext) {
        return "";
    }


/*
    private DocumentParser createDocumentParser() {
        return new DocumentParser(blockParserFactories, inlineParserFactory, delimiterProcessors, includeSourceSpans);
    }*/
}
