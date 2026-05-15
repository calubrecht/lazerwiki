package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.nodes.LineBreakNode;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LineBreakParser extends AbstractInnerParser {
    @Override
    public Collection<Character> keyCharacters() {
        return List.of(' ');
    }

    final Pattern pattern = Pattern.compile("^ \\\\\\\\"); // Two backspaces, double escape, once for java once for RE

    @Override
    public Pair<Integer, ITreeNode> parse(ParseContext parseContext) {
        int position = parseContext.getPosition();
        Matcher m = pattern.matcher(parseContext.subsequence());
        if (m.find()) {
            LineBreakNode node = new LineBreakNode();
            node.setPosition(Pair.of(position, position+2));
            return Pair.of(3, node);
        }
        return null;
    }
}
