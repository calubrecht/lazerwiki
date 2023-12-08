
package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

import java.util.List;

@Component
public class InnerTextRenderer extends TreeRenderer {
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.Inner_textContext.class, DokuwikiParser.Inner_text_nowsstartContext.class);
    }

    @Override
    public StringBuilder render(ParseTree tree, RenderContext renderContext) {
        StringBuilder outBuffer = new StringBuilder();
        StringBuilder currentBuffer = new StringBuilder();
        for(int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            TreeRenderer renderer = renderers.getRenderer(child.getClass(), child);
            StringBuilder currentRender = renderer.render(child, renderContext);
            if (renderer.shouldParentSanitize()) {
                currentBuffer.append(currentRender);
            } else {
                outBuffer.append(sanitize(currentBuffer.toString()));
                currentBuffer.setLength(0);
                outBuffer.append(currentRender);
            }
        }
        outBuffer.append(sanitize(currentBuffer.toString()));
        return outBuffer;

    }

    @Override
    public StringBuilder renderToPlainText(ParseTree tree, RenderContext context) {
        return renderChildrenToPlainText(getChildren(tree), context);
    }
}
