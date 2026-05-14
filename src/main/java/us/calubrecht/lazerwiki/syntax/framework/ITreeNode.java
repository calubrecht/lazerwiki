package us.calubrecht.lazerwiki.syntax.framework;

import org.apache.commons.lang3.tuple.Pair;

public interface ITreeNode {
    void setPosition(Pair<Integer, Integer> position);
    Pair<Integer, Integer> getPosition();

    String asString();
}
