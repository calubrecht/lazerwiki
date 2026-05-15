package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.SpecialSpanNode;
import us.calubrecht.lazerwiki.syntax.nodes.SpecialSpanNode.SPAN_TYPE;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SpecialSpanParser extends AbstractInnerParser {
    final String patternFormat = "<%s>(.*?)</%s>";

    final Pattern tagPattern = Pattern.compile("^<(sup|sub|del)>");
    final Map<String, Pattern> patternMap = new ConcurrentHashMap<>();

    final Map<String, SPAN_TYPE> typeForTag =
            Map.of("sup", SPAN_TYPE.SUP, "sub", SPAN_TYPE.SUB, "del", SPAN_TYPE.DEL);

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

        Pattern pattern = patternMap.computeIfAbsent(tagName,
                (t) -> Pattern.compile(String.format(patternFormat, t, t), Pattern.DOTALL));

        Matcher m = pattern.matcher(sequence);

        if (m.find()) {
            SpecialSpanNode node = new SpecialSpanNode(typeForTag.get(tagName));
            int length = m.group(0).length();
            node.setPosition(Pair.of(position, position + length - 1));
            ParseContext innerParseContext = new ParseContext(m.group(1), position + m.start(1));
            Parser.parseInner(innerParseContext, node, registrar);
            return Pair.of(length, node);
        }
        return null;
    }
}
