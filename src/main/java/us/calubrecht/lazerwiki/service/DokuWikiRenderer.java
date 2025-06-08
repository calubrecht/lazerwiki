package us.calubrecht.lazerwiki.service;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.HeaderRef;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiLexer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.AdditiveTreeRenderer;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.*;

import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.*;

/**
 * An implementation of IMarkupRenderer that speaks DokuWiki's markup language.
 */
@Service
public class DokuWikiRenderer implements IMarkupRenderer {

    @Autowired
    RendererRegistrar renderers;

    @Autowired
    TOCRenderService tocRenderService;

    @Override
    public String  renderToString(String markup, RenderContext context) {
        return renderToString(parseMarkup(markup), context);
    }

    ParseTree parseMarkup(String markup) {
       ParseTree tree = doParseMarkup(markup, false);
       if (tree == null) {
           return doParseMarkup(markup, true);
       }
       return tree;
    }

    ParseTree doParseMarkup(String markup, boolean allowBroken) {
        DokuwikiLexer lexer = new DokuwikiLexer(CharStreams.fromString(markup + '\n'));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        DokuwikiParser parser = new DokuwikiParser(tokens);
        if (allowBroken) {
            parser.setAllowBroken();
        }
        ParseTree tree = parser.page();
        if (!allowBroken && parser.getNumberOfSyntaxErrors() > 0) {
            return null;
        }
        return tree;
    }

    String renderToString(ParseTree tree, RenderContext context) {
        StringBuilder outBuffer = new StringBuilder();
        List<ParseTree> childrenToMerge = new ArrayList<>();
        String lastChildClass = null;
        Map<String, Object> renderState = context.renderState();
        RenderContext renderContext = new RenderContext(context.host(), context.site(), context.page(), context.user(), this, renderState);
        for(int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            TreeRenderer renderer = renderers.getRenderer(child.getClass(), child);
            if (lastChildClass != null && !lastChildClass.equals(renderer.getAdditiveClass()))
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
        renderToC(outBuffer, renderContext);
        return outBuffer.toString().strip();
    }

    private void renderToC(StringBuilder outBuffer, RenderContext renderContext) {
        List<HeaderRef> headers = (List<HeaderRef>)renderContext.renderState().getOrDefault(HEADERS.name(), Collections.emptyList());
        Object forceTOC = renderContext.renderState().get(TOC.name());
        if (Boolean.FALSE.equals(forceTOC) || (headers.size() < 3) && !Boolean.TRUE.equals(forceTOC)) {
            return;
        }
        String idSuffix = renderContext.renderState().getOrDefault(ID_SUFFIX.name(), "").toString();
        String toc = tocRenderService.renderTOC(headers, idSuffix);

        outBuffer.insert(0, toc);
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
        RenderContext plainTextContext = new RenderContext(renderContext.host(), renderContext.site(), renderContext.page(), renderContext.user(), renderContext.renderer(), new HashMap<>(renderContext.renderState()));
        plainTextContext.renderState().put("plainText", true);
        return renderToPlainText(parseMarkup(markup), renderContext);
    }

    public String renderToPlainText(ParseTree tree, RenderContext renderContext) {
        StringBuilder outBuffer = new StringBuilder();
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
