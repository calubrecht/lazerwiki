package us.calubrecht.lazerwiki.service.renderhelpers;

import ch.qos.logback.core.joran.sanity.Pair;

import java.util.Collection;

/**
 * A RenderHelper_Line that delegates content in that line to further RenderHelper_Inlines
 */
public abstract class DelegatingRenderHelper implements RenderHelper_Line{
    protected abstract Collection<RenderHelper_Inline> getDelegates();

    protected String applyDelegatesToContent(String content) {
        for (RenderHelper_Inline delegate : getDelegates() ) {
          Pair<Integer,Integer> match = delegate.matches(content);
        };
        return content;
    }
}
