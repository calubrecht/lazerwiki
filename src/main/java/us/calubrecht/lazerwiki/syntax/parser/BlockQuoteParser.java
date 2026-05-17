package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.BlockQuoteNode;

import java.util.ArrayList;
import java.util.List;

@Component
public class BlockQuoteParser extends AbstractTreeParser{
    final char KEY_CHAR = '>';

    @Override
    public ITreeNode parse(ParseContext parseContext) {
        ParseContext blockLines = new ParseContext();
        // Sub context counts from current location
        blockLines.setRoot(parseContext, parseContext.getPosition());
        int start = parseContext.getPosition();
        while(!parseContext.isEmpty()) {
            String nextLine = parseContext.peekLine();
            if (canBeginParse(nextLine)) {
                blockLines.addLine(nextLine);
                parseContext.advanceLine();
            }
            else {
                // End of block
                break;
            }
        }
        blockLines.lock();
        List<BlockQuoteNode> nodeStack = new ArrayList<>();
        BlockQuoteNode node = newNode(nodeStack, parseContext);
        node.setPosition(Pair.of(start, -1));
        node.setParseContext(parseContext);
        int nodeStart = start;
        ParseContext nodeLines = new ParseContext();
        nodeLines.setRoot(parseContext, nodeStart);
        for (String line : blockLines) {
            int count = tokenCount(line);
            while (count > nodeStack.size() ) {
                Parser.parseInner(nodeLines, node, registrar);
                nodeStart = nodeLines.getPosition();
                nodeLines = new ParseContext();
                nodeLines.setRoot(parseContext, nodeStart);
                BlockQuoteNode oldNode = node;
                node = newNode(nodeStack, parseContext);
                node.setPosition(nodeStart, -1);
                oldNode.addChild(node);
            }
            while (count < nodeStack.size() ) {
                Parser.parseInner(nodeLines, node, registrar);
                nodeStart = nodeLines.getPosition();
                nodeLines = new ParseContext();
                nodeLines.setRoot(parseContext, nodeStart);
                node.setPosition(node.getPosition().getLeft(), nodeLines.getPosition() -1);
                nodeStack.remove(0);
                node = nodeStack.get(0);
            }
            nodeLines.addLine((nodeLines.isEmpty() ? "" : " \\\\") + line.substring(count));
        }
        while (!nodeStack.isEmpty()) {
            node = nodeStack.get(0);
            if (!nodeLines.isEmpty()) {
                Parser.parseInner(nodeLines, node, registrar);
                nodeStart = nodeLines.getPosition();
                nodeLines = new ParseContext();
                nodeLines.setRoot(parseContext, nodeStart);
            }
            node.setPosition(node.getPosition().getLeft(), nodeLines.getPosition() -1);
            nodeStack.remove(0);
        }

        return node;
    }

    BlockQuoteNode newNode(List<BlockQuoteNode> nodeStack, ParseContext parseContext) {
        BlockQuoteNode node = new BlockQuoteNode();
        node.setParseContext(parseContext);
        nodeStack.add(0, node);
        return node;
    }


    int tokenCount(String line) {
        int i;
        for(i = 0; i < line.length() && line.charAt(i) == KEY_CHAR; i++){

        }
        return i;
    }

    @Override
    public boolean canBeginParse(String line) {
        return !line.isEmpty() && line.charAt(0) == KEY_CHAR;
    }

    @Override
    public String parserKey() {
        return "BlockQuote";
    }
}
