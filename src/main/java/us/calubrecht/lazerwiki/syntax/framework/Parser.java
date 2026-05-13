package us.calubrecht.lazerwiki.syntax.framework;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.nodes.ContainerNode;
import us.calubrecht.lazerwiki.syntax.nodes.TextNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class Parser {
    final ParserRegistrar parserRegistrar;

    public Parser(@Autowired ParserRegistrar parserRegistrar) {
        this.parserRegistrar = parserRegistrar;
    }

    public ITreeNode parse(String markup) {
        List<String> markupLines = new LinkedList<>(markup.lines().toList());
        ContainerNode rootNode = new ContainerNode();
        return parse(markupLines, rootNode, parserRegistrar.getParsers());
    }

    public static ITreeNode parse(String markupLine, ContainerNode container, Collection<ITreeParser> parsers) {
        return parse(new ArrayList<>(List.of(markupLine)), container, parsers);
    }

    public static ITreeNode parse(List<String> markupLines, ContainerNode container, Collection<ITreeParser> parsers) {
        while (!markupLines.isEmpty() ) {
            ITreeNode nextNode = null;
            for (ITreeParser parser: parsers) {
                nextNode = parser.parse(markupLines);
                if (nextNode != null) {
                    break;
                }
            }
            if (nextNode == null) {
                String nextLine = markupLines.get(0);
                nextNode = new TextNode(nextLine);
                markupLines.remove(0);
            }
            container.addChild(nextNode);
        }
        return container;
    }

    public static void parseInner(List<String> markupLines, ContainerNode parentNode, ParserRegistrar parserRegistrar) {
        String fullMarkup = String.join("\n", markupLines);
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < fullMarkup.length(); i++ ) {
            char c = fullMarkup.charAt(i);
            if (Character.isAlphabetic(c) || Character.isDigit(c) || Character.isWhitespace(c)) {
                buffer.append(c);
                continue;
            }
            List<IInnerParser> parsers = parserRegistrar.getParsersForKeyCharacter(c);
            boolean parseFound = false;
            for (IInnerParser parser: parsers) {
                Pair<Integer, ITreeNode> parsed = parser.parse(fullMarkup.substring(i));
                if (parsed == null) {
                    continue;
                }
                else {
                    parseFound = true;
                    parentNode.addChild(new TextNode(buffer.toString()));
                    buffer = new StringBuilder();
                    parentNode.addChild(parsed.getRight());
                    i += parsed.getLeft();
                }
            }
            if (!parseFound) {
                buffer.append(c);
            }
        }
        if (!buffer.isEmpty()) {
            parentNode.addChild(new TextNode(buffer.toString()));
        }
    }
}
