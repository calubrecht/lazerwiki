package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.List;

@Component
public class MonospaceSpanRenderer extends AbstractSpanRenderer {
    @Override
    public List<Class> getTargets() {
        return List.of(DokuwikiParser.Monospace_spanContext.class);
    }

    public MonospaceSpanRenderer() {
        super("<span class=\"monospace\">", "</span>");
    }
}
