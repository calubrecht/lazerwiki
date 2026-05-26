package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ITreeParser;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.TaggedContainerNode;

@Component
public class ParagraphParser extends AbstractTreeParser {
    @Override
    public ITreeNode parse(ParseContext parseContext) {
        ParseContext paragraphLines = new ParseContext();
        paragraphLines.setRoot(parseContext, parseContext.getPosition());
        // Sub context counts from current location
        int start = parseContext.getPosition();
        int end =  parseContext.getPosition();
        int lastEnd = end;
        for (String nextLine = parseContext.remainingLine(); !parseContext.isEmpty(); nextLine = getNext(parseContext)) {
          if (!paragraphLines.isEmpty() && nonParagraphBlock(getNext(parseContext))) {
              // End of paragraph. Preserve line for next parser
              // Always parses the first line, do not break for other parsers until this accepts first line
              end = lastEnd;
              break;
          }
          if (!nextLine.isEmpty()) {
              paragraphLines.addLine(nextLine);
              lastEnd = end;
              parseContext.advanceLine();
              end =  parseContext.getPosition();
          }
          else {
              // Blank line marks end of paragraph
              end =  parseContext.getPosition();
              parseContext.advanceLine();
              break;
          }
        }
        TaggedContainerNode node = new TaggedContainerNode(TaggedContainerNode.TYPE.PARAGRAPH);
        node.setPosition(Pair.of(start, end));
        node.setParseContext(parseContext);
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
