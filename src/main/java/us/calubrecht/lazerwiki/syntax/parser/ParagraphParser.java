package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ITreeParser;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.ParagraphNode;

@Component
public class ParagraphParser extends AbstractTreeParser {
    @Override
    public ITreeNode parse(ParseContext parseContext) {
        ParseContext paragraphLines = new ParseContext();
        paragraphLines.setRoot(parseContext.getPosition());
        // Sub context counts from current location
        int start = parseContext.getPosition();
        for (String nextLine = parseContext.peekLine(); !parseContext.isEmpty(); nextLine = getNext(parseContext)) {
          if (nonParagraphBlock(nextLine)) {
              // End of paragraph. Preserve line for next parser
              break;
          }
          if (!nextLine.isEmpty()) {
              paragraphLines.addLine(nextLine);
              parseContext.advanceLine();
          }
          else {
              // Blank line marks end of paragraph
              parseContext.advanceLine();
              break;
          }
        }
        ParagraphNode node = new ParagraphNode();
        node.setPosition(Pair.of(start, parseContext.getPosition() -1));
        Parser.parseInner(paragraphLines, node, registrar);
        return node;
    }

    @Override
    public boolean canBeginParse(String line) {
        return true; // Paragraph is final fall-through parser and will handle any line
    }

    String getNext(ParseContext parseContext) {
        return parseContext.isEmpty() ? null : parseContext.peekLine();
    }

    /**
     * Check if another block might be starting. List, code block etc. can preempt a paragraph
     */
    boolean nonParagraphBlock(String line) {
        for (ITreeParser parser : registrar.getParsers()) {
            if (parser != this && parser.canBeginParse(line)) {
                return true;
            }
        }
        return false;
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
