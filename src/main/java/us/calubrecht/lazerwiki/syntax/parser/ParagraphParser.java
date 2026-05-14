package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.ParagraphNode;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ParagraphParser extends AbstractTreeParser {
    @Override
    public ITreeNode parse(List<String> markupLines, AtomicInteger counter) {
        List<String> paragraphLines = new LinkedList<>();
        int start = counter.get();
        for (String nextLine = markupLines.get(0); !markupLines.isEmpty(); nextLine = getNext(markupLines)) {
          if (nonParagraphBlock(nextLine)) {
              // End of paragraph. Preserve line for next parser
              break;
          }
          if (!nextLine.isEmpty()) {
              paragraphLines.add(nextLine);
              markupLines.remove(0);
              counter.addAndGet(nextLine.length() + 1);
          }
          else {
              // Blank line marks end of paragraph
              markupLines.remove(0);
              counter.addAndGet( 1);
              break;
          }
        }
        ParagraphNode node = new ParagraphNode();
        node.setPosition(Pair.of(start, counter.get() -1));
        // XXX Need to introduce the concept of legal child parsers. Parser registrar
        // To provide option to look up
        Parser.parseInner(paragraphLines, node, start, registrar);
        return node;
    }

    String getNext(List<String> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Bit hack: Break a paraagraph if we hit a code block or list.
     */
    final Set<String> breakingStart = Set.of("  ", " *", " -");
    boolean nonParagraphBlock(String line) {
        String twoChar = line.length() < 2 ? line : line.substring(0,2);
        return breakingStart.contains(twoChar);
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
