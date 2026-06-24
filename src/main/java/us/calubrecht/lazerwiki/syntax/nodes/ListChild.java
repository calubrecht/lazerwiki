package us.calubrecht.lazerwiki.syntax.nodes;

public interface ListChild {

  record ListChildList(ListNode list) implements ListChild {}

  class ListItemNode extends ContainerNode implements ListChild {
    String value;

    public void setValue(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
