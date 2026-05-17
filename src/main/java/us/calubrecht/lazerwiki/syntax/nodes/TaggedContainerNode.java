package us.calubrecht.lazerwiki.syntax.nodes;

public class TaggedContainerNode extends ContainerNode {
    public enum TYPE {PARAGRAPH, CODE_BLOCK, BLOCK_QUOTE}

    TYPE type;

    public TaggedContainerNode(TYPE type) {
        this.type = type;
    }

    public TYPE getType() {
        return type;
    }
}
