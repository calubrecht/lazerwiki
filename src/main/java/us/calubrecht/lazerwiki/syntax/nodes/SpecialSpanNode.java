package us.calubrecht.lazerwiki.syntax.nodes;

public class SpecialSpanNode extends ContainerNode {
  public enum SpanType {
    SUP,
    SUB,
    DEL
  }

  final SpanType type;

  public SpecialSpanNode(SpanType type) {
    this.type = type;
  }

  public SpanType getType() {
    return type;
  }
}
