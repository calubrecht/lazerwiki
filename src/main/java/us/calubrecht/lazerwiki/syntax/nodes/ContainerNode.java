package us.calubrecht.lazerwiki.syntax.nodes;

import org.apache.commons.lang3.tuple.Pair;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A Tree node represeneting a list of nodes with no inherent meaning itself.
 */
public class ContainerNode implements ITreeNode {
    List<ITreeNode> children = new ArrayList<>();
    Pair<Integer, Integer> position = null;

    public void addChild(ITreeNode node) {
        if (node != null) {
            children.add(node);
        }
    }

    public List<ITreeNode> getChildren() {
        return children;
    }

    public void setPosition(Pair<Integer, Integer> position) {
        this.position = position;
    }
    public Pair<Integer, Integer> getPosition() {
        return position;
    }
}
