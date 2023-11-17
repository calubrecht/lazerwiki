
package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

import java.util.List;

@Component
public class InnerTextRenderer extends TreeRenderer {
    public List<Class> getTargets() {
        return List.of(DokuwikiParser.Inner_textContext.class, DokuwikiParser.Inner_text_nowsstartContext.class);
    }

    protected String sanitize(String input) {
        return StringEscapeUtils.escapeHtml4(input).replaceAll("&quot;", "\"");
    }
    public StringBuffer render(ParseTree tree) {
        StringBuffer outBuffer = new StringBuffer();
        StringBuffer currentBuffer = new StringBuffer();
        for(int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            TreeRenderer renderer = renderers.getRenderer(child.getClass());
            StringBuffer currentRender = renderer.render(child);
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
}
