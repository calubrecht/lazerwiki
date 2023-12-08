package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.AdditiveTreeRenderer;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class ListRenderer extends AdditiveTreeRenderer {
    private final String listTag;

    final Map<String, Class<? extends ParseTree>> tokenMapping = Map.of(
            "-", DokuwikiParser.Olist_itemContext.class, "*", DokuwikiParser.Ulist_itemContext.class);

    public ListRenderer(String listTag) {
        this.listTag = listTag;
    }

    ListSpec getListSpec(ParseTree tree) {
        int count = 0;
        ListSpec spec = null;

        for (int i = 0;; i++) {
            ParseTree child = tree.getChild(i);
            String token = child.getText();
            if (tokenMapping.get(token) != null) {
                ListRenderer renderer = (ListRenderer)renderers.getRenderer(tokenMapping.get(token), child);
                spec = new ListSpec(renderer.listTag, count);
                break;
            }
            count++;
        }
        return spec;
    }

    @Override
    public StringBuffer render(List<ParseTree> trees, RenderContext renderContext) {
        StringBuffer sb = new StringBuffer();
        ListSpec currentSpec = getListSpec(trees.get(0));
        sb.append("<%s>\n".formatted(currentSpec.listType()));
        while (!trees.isEmpty()) {
            ParseTree current = trees.get(0);
            ListSpec newSpec = getListSpec(current);
            if (newSpec.equals(currentSpec)) {
                sb.append(render(current, renderContext));
                trees.remove(0);
                continue;
            }
            if (newSpec.depth() == currentSpec.depth()) {
                sb.append("</%s>\n".formatted(currentSpec.listType()));
                sb.append("<%s>\n".formatted(newSpec.listType()));
                sb.append(render(current, renderContext));
                trees.remove(0);
                currentSpec = newSpec;
                continue;
            }
            if (newSpec.depth() <= currentSpec.depth()) {
                break;
            }
            // Start nested list
            sb.append(render(trees, renderContext));
        }
        sb.append("</%s>\n".formatted(currentSpec.listType()));
        return sb;
    }


    @Override
    public StringBuffer render(ParseTree tree, RenderContext renderContext) {
        StringBuffer sb = new StringBuffer();
        Optional<ParseTree> content = getChildren(tree).stream().filter(t -> t.getClass() == DokuwikiParser.Inner_textContext.class).findFirst();
        content.ifPresent(pt -> {
            TreeRenderer renderer = renderers.getRenderer(pt.getClass(), pt);
            sb.append("<li>").append(renderer.render(pt, renderContext)).append("</li>\n"); }
        );
        return sb;
    }

    @Override
    public StringBuffer renderToPlainText(ParseTree tree, RenderContext renderContext) {
        return renderChildrenToPlainText(getChildren(tree, 1, tree.getChildCount()).stream().filter(t -> ! (t instanceof TerminalNodeImpl)).collect(Collectors.toList()), renderContext);
    }

    @Override
    public boolean isAdditive() {
        return true;
    }

    @Override
    public String getAdditiveClass() {
        return "List";
    }

    record ListSpec(String listType, int depth) {}
}
