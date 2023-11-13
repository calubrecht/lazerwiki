package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.AdditiveTreeRenderer;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class ListRenderer extends AdditiveTreeRenderer {
    private String listTag;

    public ListRenderer(String listTag) {
        this.listTag = listTag;
    }

    @Override
    public StringBuffer render(List<ParseTree> trees) {
        StringBuffer ret = new StringBuffer();
        ret.append("<%s>".formatted(listTag));
        ret.append(
                trees.stream().map(t -> render(t)).collect(Collectors.joining()));
        ret.append("</%s>\n".formatted(listTag));
        return ret;
    }

    @Override
    public StringBuffer render(ParseTree tree) {
        StringBuffer sb = new StringBuffer();
        Optional<ParseTree> content = getChildren(tree).stream().filter(t -> t.getClass() == DokuwikiParser.Inner_textContext.class).findFirst();
        content.ifPresent(pt -> {
            TreeRenderer renderer = renderers.getRenderer(pt.getClass());
            sb.append("<li>" +renderer.render(pt) + "</li>\n"); }
        );
        return sb;
    }

    @Override
    public boolean isAdditive() {
        return true;
    }
}
