package us.calubrecht.lazerwiki.service;

import static us.calubrecht.lazerwiki.model.RenderResult.RenderStateKeys.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.HeaderRef;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.framework.Renderer;

/** An implementation of IMarkupRenderer that speaks DokuWiki's markup language. */
@Service
@Qualifier("New")
public class CustomWikiRenderer implements IMarkupRenderer {
  final Parser parser;
  final Renderer renderer;

  final TOCRenderService tocRenderService;

  public CustomWikiRenderer(
      @Autowired Parser parser,
      @Autowired Renderer renderer,
      @Autowired TOCRenderService tocRenderService) {
    this.parser = parser;
    this.renderer = renderer;
    this.tocRenderService = tocRenderService;
  }

  @Override
  public RenderResult renderWithInfo(String markup, RenderContext renderContext) {
    ITreeNode node = parser.parse(markup);
    RenderContext htmlContext =
        new RenderContext(
            renderContext.site(),
            renderContext.page(),
            renderContext.user(),
            this,
            renderContext.renderState());
    String rendered = renderer.render(node, htmlContext);
    String toc = renderToC(renderContext);
    RenderContext plaintextContext =
        new RenderContext(
            renderContext.site(),
            renderContext.page(),
            renderContext.user(),
            this,
            new HashMap<>());
    plaintextContext.renderState().put("plainText", true);
    String plainText = renderer.renderPlaintext(node, plaintextContext);
    return new RenderResult(toc + rendered, plainText, renderContext.renderState());
  }

  @Override
  public String renderToString(String markup, RenderContext renderContext) {
    return renderWithInfo(markup, renderContext).renderedText();
  }

  @SuppressWarnings("unchecked")
  private String renderToC(RenderContext renderContext) {
    List<HeaderRef> headers =
        (List<HeaderRef>)
            renderContext.renderState().getOrDefault(HEADERS.name(), Collections.emptyList());
    Object forceTOC = renderContext.renderState().get(TOC.name());
    if (Boolean.FALSE.equals(forceTOC) || (headers.size() < 3) && !Boolean.TRUE.equals(forceTOC)) {
      return "";
    }
    String idSuffix = renderContext.renderState().getOrDefault(ID_SUFFIX.name(), "").toString();
    return tocRenderService.renderTOC(headers, idSuffix);
  }
}
