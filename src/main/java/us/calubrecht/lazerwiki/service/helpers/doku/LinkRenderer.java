package us.calubrecht.lazerwiki.service.helpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.PageService;
import us.calubrecht.lazerwiki.service.helpers.TreeRenderer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

@Component
public class LinkRenderer extends TreeRenderer {
    static final String MISSING_LINK_CLASS ="wikiLinkMissing";
    static final String LINK_CLASS ="wikiLink";

    @Autowired
    PageService pageService;

    public Class getTarget() {
        return DokuwikiParser.LinkContext.class;
    }

    protected String getLinkTarget(ParseTree tree) {
        return tree.getChild(1).getText().strip();
    }

    protected String getLinkDisplay(ParseTree tree) {
        if (tree.getChildCount() > 3) {
            return tree.getChild(2).getText().substring(1).strip();
        }
        return "YOUR TEXT HERE";
    }

    protected String getCssClass(String targetName) {
        return pageService.exists(targetName) ? LINK_CLASS : MISSING_LINK_CLASS;
    }

    public StringBuffer render(ParseTree tree) {
        DokuwikiParser.LinkContext context = (DokuwikiParser.LinkContext)tree;
        String linkTarget = getLinkTarget(tree);
        String cssClass = getCssClass(linkTarget);
        return new StringBuffer("<a class=\"%s\" href=\"/%s\">%s</a>".
                formatted(cssClass, linkTarget, getLinkDisplay(tree)));
    }
}
