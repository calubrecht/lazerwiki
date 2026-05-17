package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.nodes.CodeBlockNode;
import us.calubrecht.lazerwiki.syntax.nodes.TextNode;

@Component
public class CodeBlockParser extends AbstractTreeParser {
    final String doubleSpace = "  ";
    @Override
    public ITreeNode parse(ParseContext parseContext) {
        ParseContext blockLines = new ParseContext();
        // Sub context counts from current location
        blockLines.setRoot(parseContext, parseContext.getPosition());
        int start = parseContext.getPosition();
        while(!parseContext.isEmpty()) {
            String nextLine = parseContext.peekLine();
            if (nextLine.startsWith(doubleSpace)) {
                blockLines.addLine(nextLine);
                parseContext.advanceLine();
            }
            else if (nextLine.isEmpty()) {
                // Empty line, break codeblock
                break;
            }
        }
        blockLines.lock();
        CodeBlockNode node = new CodeBlockNode();
        node.setPosition(Pair.of(start, blockLines.getPosition() -1));
        node.setParseContext(parseContext);
        int lineStart = start;
        for (String line : blockLines) {
            TextNode textNode = new TextNode(line.substring(2) + '\n');
            textNode.setPosition(Pair.of(lineStart+2, lineStart + line.length() - 1));
            textNode.setParseContext(parseContext);
            node.addChild(textNode);
            lineStart = blockLines.getPosition();
        }
        return node;
    }

    @Override
    public boolean canBeginParse(String line) {
        return line.startsWith(doubleSpace);
    }

    @Override
    public String parserKey() {
        return "Code";
    }

    public int priority() {
        return 110;
    } // Lower priority than List
}
