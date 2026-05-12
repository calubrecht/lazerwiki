package us.calubrecht.lazerwiki.syntax.nodes;

import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;

import java.util.List;

public class TextNode implements ITreeNode {
    String content;

    public TextNode(String content) {
        this.content = content;
    }

    @Override
    public void addChild(ITreeNode node) {
        throw new UnsupportedOperationException("Text Node cannot have children");
    }

    @Override
    public List<ITreeNode> getChildren() {
        return List.of();
    }

    public String asString() {
        return content;
    }

    public String toString() {
        return content;
    }
}
