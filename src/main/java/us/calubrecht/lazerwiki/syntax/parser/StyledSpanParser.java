package us.calubrecht.lazerwiki.syntax.parser;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.StyledSpanNode;
import us.calubrecht.lazerwiki.syntax.nodes.StyledSpanNode.SpanType;

@Component
public class StyledSpanParser extends AbstractInnerParser {
  final Pattern boldPattern = Pattern.compile("^\\*\\*(.*?)\\*\\*", Pattern.DOTALL);
  final Pattern italicPattern = Pattern.compile("^//(.*?)//", Pattern.DOTALL);
  final Pattern underlinePattern = Pattern.compile("^__(.*?)__", Pattern.DOTALL);
  final Pattern monospacePattern = Pattern.compile("^''(.*?)''", Pattern.DOTALL);

  final Map<Character, Pattern> patternForChar =
      Map.of('*', boldPattern, '/', italicPattern, '_', underlinePattern, '\'', monospacePattern);
  final Map<Character, SpanType> typeForChar =
      Map.of(
          '*',
          SpanType.BOLD,
          '/',
          SpanType.ITALIC,
          '_',
          SpanType.UNDERLINE,
          '\'',
          SpanType.MONOSPACE);

  @Override
  public Collection<Character> keyCharacters() {
    return typeForChar.keySet();
  }

  @Override
  public Pair<Integer, ITreeNode> parse(ParseContext parseContext) {
    CharSequence sequence = parseContext.subsequence();
    int position = parseContext.getPosition();
    char keyChar = parseContext.peekChar();
    Matcher m = patternForChar.get(keyChar).matcher(sequence);

    if (m.find()) {
      StyledSpanNode styledSpanNode = new StyledSpanNode(typeForChar.get(keyChar));
      int length = m.group(0).length();
      styledSpanNode.setPosition(Pair.of(position, position + length - 1));
      styledSpanNode.setParseContext(parseContext);
      ParseContext innerParseContext = new ParseContext(m.group(1), position + 2, parseContext);
      Parser.parseInner(innerParseContext, styledSpanNode, registrar);
      return Pair.of(length, styledSpanNode);
    }
    return null;
  }
}
