package us.calubrecht.lazerwiki.syntax.framework;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.nodes.ContainerNode;
import us.calubrecht.lazerwiki.syntax.nodes.TextNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
}
