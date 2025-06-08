package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.junit.jupiter.api.Test;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ErrorNodeRendererTest {

    ErrorNodeRenderer renderer = new ErrorNodeRenderer();

    @Test
    public void testRender() {
        ParseTree tree = new MockParseTree("input1");

        RenderContext renderContext = new RenderContext("host", "site", "page", "user");
        assertEquals(
                "<div class=\"parseError\"><b>ERROR:</b> Cannot parse: [input1]</div>",
                renderer.render(List.of(tree), renderContext).toString());

        ParseTree tree2 = new MockParseTree("input2");
        assertEquals(
                "<div class=\"parseError\"><b>ERROR:</b> Cannot parse: [input1input2]</div>",
                renderer.render(List.of(tree, tree2), renderContext).toString());
    }

    @Test
    public void testRenderToPlaintext() {
        ParseTree tree = new MockParseTree("input1");

        RenderContext renderContext = new RenderContext("host", "site", "page", "user");
        assertEquals(
                "ERROR: Cannot parse: [input1]\n",
                renderer.renderToPlainText(tree, renderContext).toString());
    }

    static class MockParseTree extends TerminalNodeImpl {
        String value;

        public MockParseTree(String value) {
            super(null);
            this.value = value;
        }

        public String getText() {
            return value;
        }
    }
}
