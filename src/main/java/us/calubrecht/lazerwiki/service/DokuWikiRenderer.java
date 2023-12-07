package us.calubrecht.lazerwiki.service;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiLexer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.AdditiveTreeRenderer;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;
import us.calubrecht.lazerwiki.service.renderhelpers.doku.HeaderRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of IMarkupRenderer that speaks DokuWiki's markup language.
 */
@Service
public class DokuWikiRenderer implements IMarkupRenderer {

    @Autowired
    RendererRegistrar renderers;


    @Override
    public String  renderToString(String markup, RenderContext context) {
        return renderToString(parseMarkup(markup), context);
    }

    ParseTree parseMarkup(String markup) {
        DokuwikiLexer lexer = new DokuwikiLexer(CharStreams.fromString(markup + '\n'));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        DokuwikiParser parser = new DokuwikiParser(tokens);
        return parser.page();
    }

    String renderToString(ParseTree tree, RenderContext context) {
        StringBuffer outBuffer = new StringBuffer();
        List<ParseTree> childrenToMerge = new ArrayList<>();
        String lastChildClass = null;
        Map<String, Object> renderState = context.renderState();
        RenderContext renderContext = new RenderContext(context.host(), context.site(), context.user(), this, renderState);
        for(int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            TreeRenderer renderer = renderers.getRenderer(child.getClass(), child);
            if (lastChildClass != null && lastChildClass != renderer.getAdditiveClass() )
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
        return outBuffer.toString().strip();
    }

    @Override
    public RenderResult renderWithInfo(String markup, RenderContext renderContext) {
        ParseTree tree = parseMarkup(markup);
        String rendered = renderToString(tree, renderContext);
        String plainText = renderToPlainText(tree, renderContext);
        return new RenderResult(rendered, plainText, renderContext.renderState());
    }

    @Override
    public String renderToPlainText(String markup, RenderContext renderContext) {
        return renderToPlainText(parseMarkup(markup), renderContext);
    }

    public String renderToPlainText(ParseTree tree, RenderContext renderContext) {
        StringBuffer outBuffer = new StringBuffer();
        for(int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            TreeRenderer renderer = renderers.getRenderer(child.getClass(), child);
            outBuffer.append(renderer.renderToPlainText(child, renderContext));
        }
        // Remove trailing new line
        outBuffer.deleteCharAt(outBuffer.length() -1);
        return TreeRenderer.sanitize(outBuffer.toString());
    }

}
