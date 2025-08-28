package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.RandomService;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    String getNamedStartTags(String name) {
        String id = getId();
        return "<div class=\"hidden\"><input id=\"" + id + "\" class=\"toggle\" type=\"checkbox\">"+
                "<label for=\"" + id + "\" class=\"hdn-toggle\" data-named=\"true\">" + name + "</label>"
                + "<div class=\"collapsible\">";

    }

    String getId() {
        return "hiddenToggle" + randomService.nextInt();
    }


    Pattern namePattern = Pattern.compile("\s*name=\"(.*)\"\s*");
    String getName(String attributes) {
        Matcher m = namePattern.matcher(attributes);
        if (!m.matches()) {
            return "";
        }
        return sanitize(m.group(1));
    }

    public StringBuilder render(ParseTree tree, RenderContext renderContext) {
        DokuwikiParser.HiddenContext hiddenTree = (DokuwikiParser.HiddenContext)tree;
        StringBuilder sb = new StringBuilder();
        String name = getName(hiddenTree.hidden_attributes().getText());
        if (name.isEmpty()) {
            sb.append(getStartTags());
        }
        else {
            sb.append(getNamedStartTags(name));
        }
        sb.append(renderChildren(List.of(hiddenTree.hidden_contents()), renderContext));
        sb.append(endTag);
        return sb;
    }

    @Override
    public StringBuilder renderToPlainText(ParseTree tree, RenderContext renderContext) {
        DokuwikiParser.HiddenContext hiddenTree = (DokuwikiParser.HiddenContext)tree;
        StringBuilder sb = new StringBuilder();
        String name = getName(hiddenTree.hidden_attributes().getText());
        if (!name.isEmpty()) {
            sb.append(name).append(":");
        }
        sb.append(renderChildrenToPlainText(List.of(hiddenTree.hidden_contents()), renderContext));
        return sb;
    }
}
