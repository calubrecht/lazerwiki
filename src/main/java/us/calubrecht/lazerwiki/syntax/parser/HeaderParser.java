package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ITreeParser;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.HeaderNode;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class HeaderParser extends AbstractTreeParser {
  final static char HEADER_CHAR = '=';
  final static String MIN_HEADER = StringUtils.repeat(HEADER_CHAR, 2);

  public ITreeNode parse(List<String> markupLines, AtomicInteger counter) {
      String line = markupLines.get(0).trim();
      if (!line.startsWith(MIN_HEADER)) {
           return null;
       }
      int tokenCount = 0;
      for (int i = 0; i < line.length(); i++) {
          if (line.charAt(i) != HEADER_CHAR) {
              String tokens = StringUtils.repeat(HEADER_CHAR, i);
              if (line.endsWith(tokens)) {
                  int start = counter.get();
                  int end = counter.addAndGet(markupLines.get(0).length()) -1;
                  markupLines.remove(0);
                  HeaderNode node =  new HeaderNode(7 - i);
                  node.setPosition(Pair.of(start, end));
                  Parser.parseInner(List.of(line.substring(i, line.length() - i)), node, start, registrar);
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
    public String parserKey() {
        return "Header";
    }
}
