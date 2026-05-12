package us.calubrecht.lazerwiki.syntax.nodes;

public class TextNode extends AbstractNode {
    final String content;

    public TextNode(String content) {
        this.content = content;
    }

    public String asString() {
        return toString();
    }

    public String toString() {
        return content;
    }
}
