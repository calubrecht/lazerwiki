package us.calubrecht.lazerwiki.syntax.nodes;

import org.apache.commons.lang3.tuple.Pair;

public class LinkNode extends ContainerNode{
    final String dest;
    Pair<Integer, Integer> targetPosition;

    public LinkNode(String dest) {
        this.dest = dest;
    }

    public String getDest() {
        return dest;
    }

    public void setTargetPosition(Pair<Integer, Integer> targetPosition) {
        this.targetPosition = targetPosition;
    }
    public Pair<Integer, Integer> getTargetPosition() {
        return targetPosition;
    }
}