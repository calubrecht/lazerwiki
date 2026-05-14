package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.CodeBlockNode;
import us.calubrecht.lazerwiki.syntax.nodes.TextNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CodeBlockParser extends AbstractTreeParser {
    @Override
    public ITreeNode parse(List<String> markupLines, AtomicInteger counter) {
        List<String> blockLines = new ArrayList<>();
        int start = counter.get();
        while(!markupLines.isEmpty()) {
            String nextLine = markupLines.get(0);
            if (nextLine.startsWith("  ")) {
                blockLines.add(nextLine);
                counter.addAndGet(nextLine.length() + 1);
                markupLines.remove(0);
            }
            else if (blockLines.isEmpty() ){
               // No Block Found
               return null;
            }
        }
        CodeBlockNode node = new CodeBlockNode();
        node.setPosition(Pair.of(start, counter.get()));
        int lineStart = start;
        for (String line : blockLines) {
            TextNode textNode = new TextNode(line.substring(2) + '\n');
            textNode.setPosition(Pair.of(lineStart+2, lineStart + line.length() - 1));
            node.addChild(textNode);
            lineStart += line.length() + 1;
        }
        return node;
    }

    @Override
    public String parserKey() {
        return "Code";
    }

    public int priority() {
        return 110;
    } // Lower priority than List
}
