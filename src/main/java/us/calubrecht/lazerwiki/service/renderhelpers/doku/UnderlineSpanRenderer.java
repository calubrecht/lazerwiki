package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

import java.util.List;

@Component
public class UnderlineSpanRenderer  extends AbstractSpanRenderer {
    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.Underline_spanContext.class);
    }

    public UnderlineSpanRenderer() {
        super("<span class=\"underline\">", "</span>");
    }
}
