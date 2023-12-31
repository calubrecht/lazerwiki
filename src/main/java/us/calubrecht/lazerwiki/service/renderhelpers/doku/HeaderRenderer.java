package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class HeaderRenderer extends TreeRenderer {
    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.HeaderContext.class);
    }

    protected List<ParseTree> getChildren(ParseTree tree) {
        List<ParseTree> children = new ArrayList<>();
        boolean inHeader = false;
        for (int i = 0;; i++) {
            ParseTree child = tree.getChild(i);
            if (child.getClass() == DokuwikiParser.Header_tokContext.class) {
                if (inHeader){
                    break;
                }
                inHeader = true;
                continue;
            }
            if (inHeader) {
                children.add(child);
            }
        }
        return children;
    }

    ParseTree getHeaderTok(ParseTree header) {
        return IntStream.range(0, header.getChildCount()).
                mapToObj(header::getChild).
                filter(child -> child.getClass() == DokuwikiParser.Header_tokContext.class).findFirst().get();
    }

    @Override
    public StringBuilder render(ParseTree tree, RenderContext renderContext) {
        int headerSize  = getHeaderTok(tree).getText().length();
        String hTag = "h" + (7 - headerSize);
        StringBuilder outBuffer = new StringBuilder();
        outBuffer.append("<").append(hTag).append(">");
        outBuffer.append(renderChildren(getChildren(tree), renderContext).toString().strip());
        outBuffer.append("</").append(hTag).append(">\n");
        if (!renderContext.renderState().containsKey(RenderResult.RENDER_STATE_KEYS.TITLE.name())) {
            renderContext.renderState().put(
                    RenderResult.RENDER_STATE_KEYS.TITLE.name(),
                    renderChildrenToPlainText(getChildren(tree), renderContext).toString().strip());
        }
        return outBuffer;

    }

    @Override
    public StringBuilder renderToPlainText(ParseTree tree, RenderContext renderContext) {
        return renderChildrenToPlainText(getChildren(tree), renderContext).append("\n");
    }
}
