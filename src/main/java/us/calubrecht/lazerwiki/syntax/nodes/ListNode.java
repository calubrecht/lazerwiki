package us.calubrecht.lazerwiki.syntax.nodes;

import java.util.ArrayList;
import java.util.List;

public class ListNode extends AbstractNode {
  public enum ListType {
    ORDERED,
    UNORDERED
  }

  final ListType listType;
  final List<ListChild> items = new ArrayList<>();

  public ListNode(ListType type) {
    listType = type;
  }

  public ListType getListType() {
    return listType;
  }

  public void addItem(ListChild item) {
    items.add(item);
  }

  public List<ListChild> getItems() {
    return items;
  }
}
