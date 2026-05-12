package us.calubrecht.lazerwiki.syntax.framework;

import java.util.List;

public interface ITreeNode {
    void addChild(ITreeNode node);
    public List<ITreeNode> getChildren();
}
