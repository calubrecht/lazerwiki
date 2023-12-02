package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.PageService;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.LINKS;

@Component
public class LinkRenderer extends TreeRenderer {
    Logger logger = LogManager.getLogger(getClass());
    static final String MISSING_LINK_CLASS ="wikiLinkMissing";
    static final String LINK_CLASS ="wikiLink";
    static final String EXTERNAL_LINK_CLASS ="wikiLinkExternal";

    @Autowired
    private PageService pageService;

    @Override
    public List<Class> getTargets() {
        return List.of(DokuwikiParser.LinkContext.class);
    }

    protected String getLinkTarget(ParseTree tree) {
        String rawTarget = tree.getChild(1).getText().strip();
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

    protected String getLinkDisplay(ParseTree tree, String linkTarget, RenderContext renderContext) {
        if (tree.getChildCount() > 3) {
            String linkText =  renderChildren(getChildren(tree, 2, 3), renderContext).toString();
            if (!linkText.isBlank()) {
                return linkText;
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
            return pageService.exists(host, targetName) ? LINK_CLASS : MISSING_LINK_CLASS;
        }
        return EXTERNAL_LINK_CLASS;
    }

    protected boolean isInternal(String link) {
        return !(link.toLowerCase().startsWith("https://") || link.toLowerCase().startsWith("http://"));
    }

    @Override
    public StringBuffer render(ParseTree tree, RenderContext renderContext) {
        DokuwikiParser.LinkContext context = (DokuwikiParser.LinkContext)tree;
        String linkTarget = getLinkTarget(tree);
        String linkURL = linkTarget.isBlank() ? "/" : ( isInternal(linkTarget) ? "/page/" + linkTarget : linkTarget);
        if (isInternal(linkTarget)) {
            ((Set<String>)renderContext.renderState().computeIfAbsent(LINKS.name(), (k) -> new HashSet<>())).add(linkTarget);
        }
        String cssClass = getCssClass(linkTarget, renderContext.host());
        return new StringBuffer("<a class=\"%s\" href=\"%s\">%s</a>".
                formatted(cssClass, linkURL, getLinkDisplay(tree, linkTarget, renderContext)));
    }

    @Override
    public StringBuffer renderToPlainText(ParseTree tree, RenderContext renderContext) {
        DokuwikiParser.LinkContext context = (DokuwikiParser.LinkContext)tree;
        String linkTarget = getLinkTarget(tree);
        if (tree.getChildCount() > 3) {
            StringBuffer linkText = renderChildrenToPlainText(getChildren(tree, 2, 3), renderContext);
            if (!linkText.toString().isBlank()) {
                return linkText;
            }
            // Fall through
        }
        if (isInternal(linkTarget)) {
            return new StringBuffer(pageService.getTitle(renderContext.host(), linkTarget));
        }
        return new StringBuffer(linkTarget);
    }

    @Override
    public boolean shouldParentSanitize() {
        return false;
    }

    @Component
    public static class LinkDisplayRenderer extends TreeRenderer {
        @Override
        public List<Class> getTargets() {
            return List.of(DokuwikiParser.Link_displayContext.class);
        }

        public StringBuffer render(ParseTree tree, RenderContext renderContext) {
            // Strip leading |
            return renderChildren(getChildren(tree, 1, tree.getChildCount()), renderContext);
        }

        @Override
        public StringBuffer renderToPlainText(ParseTree tree, RenderContext renderContext) {
            return renderChildrenToPlainText(getChildren(tree, 1, tree.getChildCount()), renderContext);
        }
    }
}
