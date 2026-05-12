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
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != HEADER_CHAR) {
                int start = parseContext.getPosition();
                parseContext.advanceLine();
                int end = parseContext.getPosition() - 2;
                HeaderNode node = new HeaderNode(7 - i);
                node.setPosition(Pair.of(start, end));
                node.setParseContext(parseContext);
                ParseContext innerParseContext = new ParseContext();
                int trailingTokens = countTrailingTokens(line);
                innerParseContext.addLine(line.substring(i, line.length() - trailingTokens));
                innerParseContext.setRoot(parseContext, start + trailingTokens);
                innerParseContext.lock();
                Parser.parseInner(innerParseContext, node, registrar);
                return node;
            }
        }
        return null;
    }

    int countTrailingTokens(String s) {
        int c = 0;
        int i = s.length() -1;
        while(true) {
            if (s.charAt(i) != HEADER_CHAR) {
                return c;
            }
            i--; c++;
        }
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
