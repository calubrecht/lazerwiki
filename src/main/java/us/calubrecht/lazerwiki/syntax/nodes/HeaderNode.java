package us.calubrecht.lazerwiki.syntax.nodes;

public class HeaderNode extends ContainerNode {
  final int level;

  public HeaderNode(int level) {
    this.level = level;
  }

  public int getLevel() {
    return level;
  }
}
