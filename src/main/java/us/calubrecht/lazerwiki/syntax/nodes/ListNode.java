package us.calubrecht.lazerwiki.syntax.nodes;

import java.util.ArrayList;
import java.util.List;

public class ListNode extends AbstractNode{
    public enum LIST_TYPE {ORDERED, UNORDERED}

    final LIST_TYPE listType;
    final List<ListChild> items = new ArrayList<>();

    public ListNode(LIST_TYPE type) {
        listType = type;
    }

    public LIST_TYPE getListType() {
        return listType;
    }

    public void addItem(ListChild item) {
        items.add(item);
    }

    public List<ListChild> getItems() {
        return items;
    }
}
