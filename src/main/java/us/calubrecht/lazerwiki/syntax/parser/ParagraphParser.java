package us.calubrecht.lazerwiki.syntax.parser;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ITreeParser;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.framework.ParserRegistrar;
import us.calubrecht.lazerwiki.syntax.nodes.ContainerNode;
import us.calubrecht.lazerwiki.syntax.nodes.ParagraphNode;

import java.util.LinkedList;
import java.util.List;

@Component
public class ParagraphParser extends AbstractTreeParser {
    @Override
    public ITreeNode parse(List<String> markupLines) {
        List<String> paragraphLines = new LinkedList<>();
        for (String nextLine = markupLines.get(0); !markupLines.isEmpty(); nextLine = getNext(markupLines)) {
          if (!nextLine.isEmpty()) {
              paragraphLines.add(nextLine);
              markupLines.remove(0);
          }
          else {
              break;
          }
        }
        ParagraphNode node = new ParagraphNode();
        // XXX Need to introduce the concept of legal child parsers. Parser registrar
        // To provide option to look up
        //Parser.parse(paragraphLines, node, List.of());
        Parser.parseInner(paragraphLines, node, registrar);
        return node;
    }

    String getNext(List<String> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public String parserKey() {
        return "Paragraph";
    }

    @Override
    public int priority() {
        // Highest value is lowest priority.
        return Integer.MAX_VALUE;
    }
}
