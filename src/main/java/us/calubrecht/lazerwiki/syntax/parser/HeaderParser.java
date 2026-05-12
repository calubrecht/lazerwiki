package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ITreeParser;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.HeaderNode;

import java.util.List;

@Component
public class HeaderParser implements ITreeParser {
  final static char HEADER_CHAR = '=';
  final static String MIN_HEADER = StringUtils.repeat(HEADER_CHAR, 2);

  public ITreeNode parse(List<String> markupLines) {
      String line = markupLines.get(0).trim();
      if (!line.startsWith(MIN_HEADER)) {
           return null;
       }
       //StringUtils.repeat(HEADER_CHAR, starCount))
      int tokenCount = 0;
      for (int i = 0; i < line.length(); i++) {
          if (line.charAt(i) != HEADER_CHAR) {
              String tokens = StringUtils.repeat(HEADER_CHAR, i);
              if (line.endsWith(tokens)) {
                  markupLines.remove(0);
                  HeaderNode node =  new HeaderNode(7 - i);
                  Parser.parse(line.substring(i, line.length() - i), node, List.of());
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
  /*  private final Heading block = new Heading();

    SourceLines content;

    public HeaderParser(int level, SourceLine content) {
        block.setLevel(level);
        this.content = SourceLines.of(content);
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public BlockContinue tryContinue(ParserState parserState) {
        return BlockContinue.none();
    }

    @Override
    public void parseInlines(InlineParser inlineParser) {
        inlineParser.parse(content, block);
    }

    public static class Factory extends AbstractBlockParserFactory {
        final static char HEADER_CHAR = '=';
        final static String MIN_HEADER = StringUtils.repeat(HEADER_CHAR, 2);


        @Override
        public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
            if (state.getIndent() >= Parsing.CODE_BLOCK_INDENT) {
                return BlockStart.none();
            }

            SourceLine line = state.getLine();
            int nextNonSpace = state.getNextNonSpaceIndex();
            Scanner scanner = Scanner.of(SourceLines.of(line.substring(nextNonSpace, line.getContent().length())));

            int starCount = scanner.matchMultiple(HEADER_CHAR);
            if (starCount < 2 || !line.getContent().toString().trim().endsWith(StringUtils.repeat(HEADER_CHAR, starCount))) {
                return BlockStart.none();
            }

            String content = line.getContent().toString().substring(nextNonSpace + starCount, line.getContent().length() - starCount);
            SourceSpan span = SourceSpan.of(0, nextNonSpace + starCount, content.length());

            return BlockStart.of(new HeaderParser(7 - starCount, SourceLine.of(content, span)));
        }
    }*/
}
