package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TypedRenderer;

import java.util.List;

@Component
public class ControlRowRenderer extends TypedRenderer<DokuwikiParser.Control_rowContext> {

    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.Control_rowContext.class);
    }

    @Override
    public StringBuilder renderContext(DokuwikiParser.Control_rowContext context, RenderContext renderContext) {
        if (!context.YES_TOC_TOKEN().isEmpty()) {
            renderContext.renderState().put(RenderResult.RENDER_STATE_KEYS.TOC.name(), true);
        } else //if (!context.NO_TOC_TOKEN().isEmpty()) {
        {
            renderContext.renderState().put(RenderResult.RENDER_STATE_KEYS.TOC.name(), false);
        }
        return new StringBuilder();
    }

    @Override
    public StringBuilder renderContextToPlainText(DokuwikiParser.Control_rowContext context, RenderContext renderContext) {
        return new StringBuilder();
    }
}
