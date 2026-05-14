package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.calubrecht.lazerwiki.syntax.nodes.MacroNode;

@Component
public class MacroParser extends AbstractInnerParser {
    final Pattern macroPattern = Pattern.compile("^~~MACRO~~(.*?)~~/MACRO~~", Pattern.DOTALL);

    @Override
    public Collection<Character> keyCharacters() {
        return List.of('~');
    }

    @Override
    public Pair<Integer, ITreeNode> parse(String markup, int start) {
        Matcher matcher = macroPattern.matcher(markup);
        if (matcher.find()) {
            String text = matcher.group(1);
            String fullText = matcher.group();
            int length = fullText.length();
            MacroNode node = new MacroNode(text, fullText);
            node.setPosition(Pair.of(start, start + length - 1));
            return Pair.of(length, node);
        }
        return null;
    }
}
