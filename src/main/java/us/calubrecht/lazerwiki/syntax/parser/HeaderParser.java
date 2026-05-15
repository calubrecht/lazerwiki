package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.HeaderNode;

@Component
public class HeaderParser extends AbstractTreeParser {
  final static char HEADER_CHAR = '=';
  final static String MIN_HEADER = StringUtils.repeat(HEADER_CHAR, 2);

  public ITreeNode parse(ParseContext parseContext) {
      String line = parseContext.peekLine().strip();
      if (!line.startsWith(MIN_HEADER)) {
           return null;
       }
      for (int i = 0; i < line.length(); i++) {
          if (line.charAt(i) != HEADER_CHAR) {
              String tokens = StringUtils.repeat(HEADER_CHAR, i);
              if (line.endsWith(tokens)) {
                  int start = parseContext.getPosition();
                  parseContext.advanceLine();
                  int end = parseContext.getPosition() - 2;
                  HeaderNode node =  new HeaderNode(7 - i);
                  node.setPosition(Pair.of(start, end));
                  ParseContext innerParseContext = new ParseContext();
                  innerParseContext.addLine(line.substring(i, line.length() - i));
                  innerParseContext.setRoot(start + i);
                  innerParseContext.lock();
                  Parser.parseInner(innerParseContext, node, registrar);
                  return node;
              }
              else {
                  return null;
              }
          }
      }
      return null;
  }

    @Override
    public boolean canBeginParse(String line) {
        line = line.strip();
        return line.startsWith("==") && line.endsWith("==");
    }

    @Override
    public String parserKey() {
        return "Header";
    }
}
