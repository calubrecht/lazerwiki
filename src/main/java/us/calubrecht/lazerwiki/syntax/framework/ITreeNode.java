package us.calubrecht.lazerwiki.syntax.framework;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface ITreeNode {
    void addChild(ITreeNode node);
    List<ITreeNode> getChildren();

    void setPosition(Pair<Integer, Integer> position);
    Pair<Integer, Integer> getPosition();

    String asString();
}
