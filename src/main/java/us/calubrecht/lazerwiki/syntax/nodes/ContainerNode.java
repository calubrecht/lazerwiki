package us.calubrecht.lazerwiki.syntax.nodes;

import java.util.ArrayList;
import java.util.List;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;

/** A Tree node represeneting a list of nodes with no inherent meaning itself. */
public class ContainerNode extends AbstractNode {
  final List<ITreeNode> children = new ArrayList<>();

  public void addChild(ITreeNode node) {
    if (node != null) {
      children.add(node);
    }
  }

  public List<ITreeNode> getChildren() {
    return children;
  }
}
