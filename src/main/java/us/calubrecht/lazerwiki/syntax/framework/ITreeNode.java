package us.calubrecht.lazerwiki.syntax.framework;

import java.util.List;

public interface ITreeNode {
    void addChild(ITreeNode node);
    List<ITreeNode> getChildren();

    default String asString() {
        throw new UnsupportedOperationException("No default text representation");
    }
}
