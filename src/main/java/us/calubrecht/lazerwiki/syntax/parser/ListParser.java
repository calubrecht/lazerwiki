package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.nodes.ListChild;
import us.calubrecht.lazerwiki.syntax.nodes.ListNode;
import us.calubrecht.lazerwiki.syntax.nodes.ListNode.LIST_TYPE;
import us.calubrecht.lazerwiki.syntax.framework.Parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ListParser extends AbstractTreeParser {
    final Pattern pattern = Pattern.compile("^(\\s+)([\\-*])(\\{\\{([0-9]+)}})?");

    @Override
    public ITreeNode parse(ParseContext parseContext) {
        int start = parseContext.getPosition();
        ListNode node = null;
        int depth = -1;
        String listToken = null;
        for (String nextLine = parseContext.peekLine(); !parseContext.isEmpty(); nextLine = getNext(parseContext)) {
            Matcher m = pattern.matcher(nextLine);
            if (!m.find()) {
                // List is over, cleanup.
                return finishList(node, start, parseContext.getPosition());
            }
            int lineDepth = m.group(1).length();
            String newToken = m.group(2);
            if (lineDepth < depth || lineDepth == depth && !listToken.equals(newToken)) {
                // Close out this list;
                return finishList(node, start, parseContext.getPosition());
            }
            if (node != null && lineDepth > depth) {
                //ListNode innerList =
                // Define a sublist[
                // more looping
                ListNode list = (ListNode)parse(parseContext);
                node.addItem(new ListChild.ListChildList(list));
            } else {
                listToken = newToken;
                depth = lineDepth;
                if (node == null) {
                    node = new ListNode(listToken.equals("*" ) ? LIST_TYPE.UNORDERED : LIST_TYPE.ORDERED);
                    node.setParseContext(parseContext);
                }
                int length = m.group().length();
                String itemText = nextLine.substring(length);
                int itemStart = parseContext.getPosition() + length;
                parseContext.advanceLine();
                int lineEnd = parseContext.getPosition() - -1;
                ListChild.ListItemNode item = new ListChild.ListItemNode();
                item.setPosition(Pair.of(itemStart, lineEnd));
                item.setParseContext(parseContext);
                if (listToken.equals("-") ) {
                    // An item value was provided. Only handle if Ordered
                    item.setValue(m.group(4));
                }
                ParseContext itemContext = new ParseContext(itemText, itemStart, parseContext);
                Parser.parseInner(itemContext, item, registrar);
                node.addItem(item);
            }
        }
        return finishList(node, start, parseContext.getPosition());
    }

    @Override
    public boolean canBeginParse(String line) {
        if (line.isBlank() || line.charAt(0) != ' ') {
            return false;
        }
        String afterSpaces = line.stripLeading();
        return afterSpaces.startsWith("*") || afterSpaces.startsWith("-");
    }

    ListNode finishList(ListNode node, int start, int end) {
        node.setPosition(Pair.of(start, end -1));
        return node;
    }

    String getNext(ParseContext parseContext) {
        return parseContext.isEmpty() ? null : parseContext.peekLine();
    }

    @Override
    public String parserKey() {
        return "List";
    }
}
