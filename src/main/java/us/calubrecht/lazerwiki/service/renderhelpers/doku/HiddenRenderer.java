package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.RandomService;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.List;
import java.util.Random;

@Component
public class HiddenRenderer extends TreeRenderer {
    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.HiddenContext.class);
    }

    final String endTag;

    @Autowired
    RandomService randomService;

    public HiddenRenderer() {
        endTag = "</div></div>";
    }

    String getStartTags() {
        String id = getId();
        return "<div class=\"hidden\"><input id=\"" + id + "\" class=\"toggle\" type=\"checkbox\">"+
                "<label for=\"" + id + "\" class=\"hdn-toggle\">Hidden</label>"
                + "<div class=\"collapsible\">";

    }

    String getId() {
        return "hiddenToggle" + randomService.nextInt();
    }

    public StringBuilder render(ParseTree tree, RenderContext renderContext) {
        StringBuilder sb = new StringBuilder();
        sb.append(getStartTags());
        sb.append(renderChildren(getChildren(tree, 1, tree.getChildCount()), renderContext));
        sb.append(endTag);
        return sb;
    }

    @Override
    public StringBuilder renderToPlainText(ParseTree tree, RenderContext renderContext) {
        StringBuilder sb = new StringBuilder();
        sb.append(renderChildrenToPlainText(getChildren(tree, 1, tree.getChildCount()), renderContext));
        return sb;
    }
}
