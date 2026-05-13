package us.calubrecht.lazerwiki.syntax.nodes;

public class StyledSpanNode extends ContainerNode {
    public enum SPAN_TYPE {BOLD, ITALIC, UNDERLINE, MONOSPACE};

    SPAN_TYPE type;

    public StyledSpanNode(SPAN_TYPE type) {
        this.type = type;
    }

    public SPAN_TYPE getType() {
        return type;
    }
}
