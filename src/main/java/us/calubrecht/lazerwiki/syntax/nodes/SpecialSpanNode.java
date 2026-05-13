package us.calubrecht.lazerwiki.syntax.nodes;

public class SpecialSpanNode extends ContainerNode {
    public enum SPAN_TYPE {SUP, SUB, DEL};

    SPAN_TYPE type;

    public SpecialSpanNode(SPAN_TYPE type) {
        this.type = type;
    }

    public SPAN_TYPE getType() {
        return type;
    }
}
