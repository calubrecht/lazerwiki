package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import us.calubrecht.lazerwiki.service.renderhelpers.AdditiveTreeRenderer;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class FlatteningRenderer extends AdditiveTreeRenderer {

    abstract Set<Class<? extends ParseTree>> getTreesToFlatten();

    List<ParseTree> flattenChildren(ParseTree tree, boolean recursive) {
        List<ParseTree> trees = new ArrayList<>();
        TreeRenderer lastRenderer = null;
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree t = tree.getChild(i);
            if (getTreesToFlatten().contains(t.getClass())) {
                for (int j = 0; j < t.getChildCount(); j++) {
                    ParseTree child = t.getChild(j);
                    if (recursive && getTreesToFlatten().contains(child.getClass())) {
                        trees.addAll(flattenChildren(child, true));
                    }
                    else {
                        trees.add(child);
                    }
                    lastRenderer = renderers.getRenderer(child.getClass(), child);
                }
            } else {
                if (lastRenderer != null && lastRenderer.isAdditive()) {
                    // Additive renders are always alone in the row, so the following is alwayas an EOL
                    //if (isEOL(t)) {
                    continue;
                    //}
                }
                trees.add(t);
                lastRenderer = renderers.getRenderer(t.getClass(), t);
            }
        }
        return trees;
    }
}
