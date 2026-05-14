package us.calubrecht.lazerwiki.syntax.nodes;

import org.apache.commons.lang3.tuple.Pair;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;

import java.util.List;

public abstract class AbstractNode implements ITreeNode {
    Pair<Integer, Integer> position = null;

    @Override
    public void setPosition(Pair<Integer, Integer> position) {
        this.position = position;
    }

    @Override
    public Pair<Integer, Integer> getPosition() {
        return position;
    }

    @Override
    public String asString() {
        throw new UnsupportedOperationException("No default text representation");
    }
}
