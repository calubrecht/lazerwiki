package us.calubrecht.lazerwiki.syntax.nodes;

import org.apache.commons.lang3.tuple.Pair;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;

public class LinkNode extends ContainerNode {
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

  public String getTargetSourceFromContext() {
    if (getParseContext() == null || getPosition() == null) {
      // Node has not been fully initialized, cannot get source
      return null;
    }
    ParseContext context = getParseContext();
    Pair<Integer, Integer> position = getTargetPosition();
    // Position uses inclusive endpoints, substring's end is exclusive
    return context.getFullText().substring(position.getLeft(), position.getRight() + 1);
  }
}
