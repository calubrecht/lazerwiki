package us.calubrecht.lazerwiki.syntax.nodes;

import java.util.ArrayList;
import java.util.List;

public class ListNode extends AbstractNode{
    public enum LIST_TYPE {ORDERED, UNORDERED};

    final LIST_TYPE listType;
    List<ListItemNode> items = new ArrayList<>();

    public ListNode(LIST_TYPE type) {
        listType = type;
    }

    public LIST_TYPE getListType() {
        return listType;
    }

    public void addItem(ListItemNode item) {
        items.add(item);
    }

    public List<ListItemNode> getItems() {
        return items;
    }
}
