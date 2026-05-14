package us.calubrecht.lazerwiki.syntax.framework;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.nodes.ContainerNode;
import us.calubrecht.lazerwiki.syntax.nodes.TextNode;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class Parser {
    final ParserRegistrar parserRegistrar;

    public Parser(@Autowired ParserRegistrar parserRegistrar) {
        this.parserRegistrar = parserRegistrar;
    }

    public ITreeNode parse(String markup) {
        ParseContext parseContext = new ParseContext(markup);
        ContainerNode rootNode = new ContainerNode();
        return parse(parseContext, rootNode, parserRegistrar.getParsers());
    }

    public static ITreeNode parse(ParseContext parseContext, ContainerNode container, Collection<ITreeParser> parsers) {
        AtomicInteger counter = new AtomicInteger(0);
        while (!parseContext.isEmpty() ) {
            ITreeNode nextNode = null;
            for (ITreeParser parser: parsers) {
                if (!parser.canBeginParse(parseContext.peekLine())) {
                    continue;
                }
                nextNode = parser.parse(parseContext, counter);
                if (nextNode != null) {
                    break;
                }
            }
            if (nextNode == null) {
                int nodeStart = parseContext.getPosition();
                String nextLine = parseContext.peekLine();
                nextNode = new TextNode(parseContext.peekLine());
                parseContext.advanceLine();
                nextNode.setPosition(Pair.of(nodeStart, nodeStart + nextLine.length() -1));
            }
            container.addChild(nextNode);
        }
        return container;
    }

    public static void parseInner(List<String> markupLines, ContainerNode parentNode, int start, ParserRegistrar parserRegistrar) {
        String fullMarkup = String.join("\n", markupLines);
        StringBuilder buffer = new StringBuilder();
        int textStart = start;
        for (int i = 0; i < fullMarkup.length(); i++ ) {
            char c = fullMarkup.charAt(i);
            if (Character.isAlphabetic(c) || Character.isDigit(c)) {
                buffer.append(c);
                continue;
            }
            List<IInnerParser> parsers = parserRegistrar.getParsersForKeyCharacter(c);
            boolean parseFound = false;
            for (IInnerParser parser: parsers) {
                Pair<Integer, ITreeNode> parsed = parser.parse(fullMarkup.substring(i), i + start);
                if (parsed != null) {
                    parseFound = true;
                    parentNode.addChild(textNode(buffer, textStart));
                    buffer = new StringBuilder();
                    parentNode.addChild(parsed.getRight());
                    i += parsed.getLeft() - 1;
                    textStart  = start + i + 1;
                }
            }
            if (!parseFound) {
                buffer.append(c);
            }
        }
        parentNode.addChild(textNode(buffer, textStart));
    }

    static TextNode textNode(StringBuilder buffer, int position) {
        if (buffer.isEmpty()) {
            return null;
        }
        TextNode node = new TextNode(buffer.toString());
        node.setPosition(Pair.of(position, position + node.asString().length() - 1));
        return node;
    }
}
