package us.calubrecht.lazerwiki.syntax.nodes;

public class StyledSpanNode extends ContainerNode {
  public enum SpanType {
    BOLD,
    ITALIC,
    UNDERLINE,
    MONOSPACE
  }

  final SpanType type;

  public StyledSpanNode(SpanType type) {
    this.type = type;
  }

  public SpanType getType() {
    return type;
  }
}
