package us.calubrecht.lazerwiki.syntax.parser;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.SpecialSpanNode;
import us.calubrecht.lazerwiki.syntax.nodes.SpecialSpanNode.SpanType;

@Component
public class SpecialSpanParser extends AbstractInnerParser {
  static final String PATTERN_FORMAT = "<%s>(.*?)</%s>";

  final Pattern tagPattern = Pattern.compile("^<(sup|sub|del)>");
  final Map<String, Pattern> patternMap = new ConcurrentHashMap<>();

  final Map<String, SpanType> typeForTag =
      Map.of("sup", SpanType.SUP, "sub", SpanType.SUB, "del", SpanType.DEL);

  @Override
  public Collection<Character> keyCharacters() {
    return List.of('<');
  }

  @Override
  public Pair<Integer, ITreeNode> parse(ParseContext parseContext) {
    CharSequence sequence = parseContext.subsequence();
    int position = parseContext.getPosition();
    Matcher tagMatcher = tagPattern.matcher(sequence);

    if (!tagMatcher.find()) {
      return null;
    }
    String tagName = tagMatcher.group(1);

    Pattern pattern =
        patternMap.computeIfAbsent(
            tagName, (t) -> Pattern.compile(String.format(PATTERN_FORMAT, t, t), Pattern.DOTALL));

    Matcher m = pattern.matcher(sequence);

    if (m.find()) {
      SpecialSpanNode node = new SpecialSpanNode(typeForTag.get(tagName));
      int length = m.group(0).length();
      node.setPosition(Pair.of(position, position + length - 1));
      node.setParseContext(parseContext);
      ParseContext innerParseContext =
          new ParseContext(m.group(1), position + m.start(1), parseContext);
      Parser.parseInner(innerParseContext, node, registrar);
      return Pair.of(length, node);
    }
    return null;
  }
}
