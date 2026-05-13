package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.StyledSpanNode;
import us.calubrecht.lazerwiki.syntax.nodes.StyledSpanNode.SPAN_TYPE;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StyledSpanParser extends AbstractInnerParser {
    final Pattern boldPattern = Pattern.compile("^\\*\\*(.*?)\\*\\*", Pattern.DOTALL);
    final Pattern italicPattern = Pattern.compile("^//(.*?)//", Pattern.DOTALL);
    final Pattern underlinePattern = Pattern.compile("^__(.*?)__", Pattern.DOTALL);
    final Pattern monospacePattern = Pattern.compile("^''(.*?)''", Pattern.DOTALL);

    Map<Character, Pattern> patternForChar = Map.of('*', boldPattern, '/', italicPattern, '_', underlinePattern, '\'', monospacePattern);
    Map<Character, SPAN_TYPE> typeForChar = Map.of('*', SPAN_TYPE.BOLD, '/', SPAN_TYPE.ITALIC, '_', SPAN_TYPE.UNDERLINE, '\'', SPAN_TYPE.MONOSPACE);

    @Override
    public Collection<Character> keyCharacters() {
        return typeForChar.keySet();
    }

    @Override
    public Pair<Integer, ITreeNode> parse(String markup, int position) {
        char keyChar = markup.charAt(0);
        Matcher m = patternForChar.get(keyChar).matcher(markup);

        if (m.find()) {
            StyledSpanNode styledSpanNode = new StyledSpanNode(typeForChar.get(keyChar));
            int length = m.group(0).length();
            styledSpanNode.setPosition(Pair.of(position, position + length - 1));
            Parser.parseInner(List.of(m.group(1)), styledSpanNode, position+2, registrar);
            return Pair.of(length, styledSpanNode);
        }
        return null;
    }
}
