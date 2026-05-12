package us.calubrecht.lazerwiki.service.custommark;

import org.apache.commons.lang3.StringUtils;
import org.commonmark.internal.HeadingParser;
import org.commonmark.internal.inline.Position;
import org.commonmark.internal.inline.Scanner;
import org.commonmark.internal.util.Parsing;
import org.commonmark.node.Block;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.SourceSpan;
import org.commonmark.parser.InlineParser;
import org.commonmark.parser.SourceLine;
import org.commonmark.parser.SourceLines;
import org.commonmark.parser.block.*;

public class HeaderParser extends AbstractBlockParser {
    private final Heading block = new Heading();

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
    }
}
