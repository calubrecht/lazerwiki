package us.calubrecht.lazerwiki.syntax.renderer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.LinkOverrideService;
import us.calubrecht.lazerwiki.service.PageService;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.LinkNode;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

@Component
public class LinkRenderer extends ContainerRenderer{
    final Logger logger = LogManager.getLogger(getClass());
    static final String MISSING_LINK_CLASS ="wikiLinkMissing";
    static final String LINK_CLASS ="wikiLink";
    static final String EXTERNAL_LINK_CLASS ="wikiLinkExternal";

    @Autowired
    private PageService pageService;

    @Autowired
    LinkOverrideService linkOverrideService;

    @Override
    public Collection<Class> getTargets() {
        return List.of(LinkNode.class);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
      LinkNode link = (LinkNode)node;
      StringBuilder buffer = new StringBuilder();
      String linkTarget = getLinkTarget(link.getDest());
      String linkURL = linkTarget.isBlank() ? "/" : ( isInternal(linkTarget) ? "/page/" + linkTarget : linkTarget);
      buffer.append("<a class=\"").append(getCssClass(link.getDest(), renderContext.host())).append("\" href=\"").append(linkURL).append("\">");
      buffer.append(getLinkDisplay(link, linkTarget, renderContext));
      buffer.append("</a>");
      return buffer;
    }

    protected String getLinkTarget(String rawTarget) {
        rawTarget = rawTarget.strip();
        if (!isInternal(rawTarget)) {
            try {
                URI uri = new URI(rawTarget);
                return uri.toString();
            } catch (URISyntaxException e) {
                logger.error("Could not format URL of form: " + rawTarget);
                return "http://malformed.invalid";
            }
        }
        String ret = rawTarget.replaceAll("[ \"=]+", "_");
        if (! ret.equals(rawTarget)) {
            logger.warn("Substitutions made when formatting URL: " + rawTarget);
        }
        return ret;
    }

    protected CharSequence getLinkDisplay(LinkNode node, String linkTarget, RenderContext renderContext) {
        if (!node.getChildren().isEmpty()) {
            StringBuilder rendered =  super.renderHtml(node, renderContext);
            if (!rendered.isEmpty()) {
                return rendered;
            }
            // Fall through
        }
        if (isInternal(linkTarget)) {
            return pageService.getTitle(renderContext.host(), linkTarget);
        }
        return linkTarget;
    }

    protected String getCssClass(String targetName, String host) {
        if (isInternal(targetName)) {
            boolean exists =  pageService.exists(host, targetName);
            return exists? LINK_CLASS : MISSING_LINK_CLASS;

        }
        return EXTERNAL_LINK_CLASS;
    }

    protected boolean isInternal(String link) {
        return !(link.toLowerCase().startsWith("https://") || link.toLowerCase().startsWith("http://"));
    }

    //<a class="wikiLinkMissing" href="/page/missing">This link is missing</a>
}
