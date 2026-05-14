package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.ListItemNode;
import us.calubrecht.lazerwiki.syntax.nodes.ListNode;
import us.calubrecht.lazerwiki.syntax.nodes.ListNode.LIST_TYPE;
import us.calubrecht.lazerwiki.syntax.framework.Parser;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ListParser extends AbstractTreeParser {
    Pattern pattern = Pattern.compile("^(\\s+)([\\-*])");

    @Override
    public ITreeNode parse(List<String> markupLines, AtomicInteger counter) {
        int start = counter.get();
        ListNode node = null;
        int depth = -1;
        String listToken = null;
        for (String nextLine = markupLines.get(0); !markupLines.isEmpty(); nextLine = getNext(markupLines)) {
            Matcher m = pattern.matcher(nextLine);
            if (!m.find()) {
                if (node == null) {
                    // No list lines matched. Skip this parser;
                    return null;
                }
                // List is over, cleanup.
                return finishList(node, start, counter);
            }
            int lineDepth = m.group(1).length();
            String newToken = m.group(2);
            if (lineDepth < depth || lineDepth == depth && !listToken.equals(newToken)) {
                // Close out this list;
                return finishList(node, start, counter);
            }
            if (node != null && lineDepth > depth) {
                //ListNode innerList =
                // Define a sublist[
                // more looping
                node = node;
            } else {
                listToken = newToken;
                depth = lineDepth;
                if (node == null) {
                    node = new ListNode(listToken.equals("*" ) ? LIST_TYPE.UNORDERED : LIST_TYPE.ORDERED);
                }
                String itemText = nextLine.substring(lineDepth + 1);
                markupLines.remove(0);
                int itemStart = counter.get() + lineDepth + 1;
                int lineEnd = counter.addAndGet(nextLine.length() + 1) - 1;
                ListItemNode item = new ListItemNode();
                item.setPosition(Pair.of(itemStart, lineEnd));
                Parser.parseInner(List.of(itemText), item, itemStart, registrar);
                node.addItem(item);
            }
        }
        return finishList(node, start, counter);
    }

    ListNode finishList(ListNode node, int start, AtomicInteger counter) {
        node.setPosition(Pair.of(start, counter.get()));
        return node;
    }

    String getNext(List<String> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public String parserKey() {
        return "List";
    }
}
