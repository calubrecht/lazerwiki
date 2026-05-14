package us.calubrecht.lazerwiki.syntax.parser;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TableParser extends AbstractTreeParser {
    final Pattern tablePattern = Pattern.compile("[|^].*[|^]");

    @Override
    public ITreeNode parse(List<String> markupLines, AtomicInteger counter) {
        List<String> tableLines = new ArrayList<>();
        int start = counter.get();
        while(!markupLines.isEmpty()) {
            String nextLine = markupLines.get(0);
            Matcher m = tablePattern.matcher(nextLine);
            if (nextLine.matches("  ")) {
                tableLines.add(nextLine);
                counter.addAndGet(nextLine.length() + 1);
                markupLines.remove(0);
            }
            else if (tableLines.isEmpty() ){
               // No table Found
               return null;
            }
        }
        TableNode node = new TableNode();
        node.setPosition(Pair.of(start, counter.get()));
        int lineStart = start;
        for (String line : tableLines) {
            // Create a TableNode.TableRowNode
            // Split row into cells
            // For each, Create a TableNode.TableCell (containerNode)
            // Then run parse inside.
            TextNode textNode = new TextNode(line.substring(2) + '\n');
            textNode.setPosition(Pair.of(lineStart+2, lineStart + line.length() - 1));
            node.addChild(textNode);
            lineStart += line.length() + 1;
        }
        return node;
    }

    @Override
    public String parserKey() {
        return "Table";
    }
}
