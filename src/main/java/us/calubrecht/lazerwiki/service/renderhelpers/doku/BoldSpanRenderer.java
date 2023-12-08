package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

import java.util.List;

@Component
public class BoldSpanRenderer extends AbstractSpanRenderer {
    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.Bold_spanContext.class);
    }

    public BoldSpanRenderer() {
        super("<span class=\"bold\">", "</span>");
    }
}
