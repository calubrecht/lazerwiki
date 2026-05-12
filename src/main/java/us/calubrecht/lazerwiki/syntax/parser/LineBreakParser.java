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

    @Override
    public Pair<Integer, ITreeNode> parse(ParseContext parseContext) {
        int position = parseContext.getPosition();
        if (parseContext.remainingStartsWith(" \\\\")) {
            LineBreakNode node = new LineBreakNode();
            node.setPosition(Pair.of(position, position+2));
            node.setParseContext(parseContext);
            return Pair.of(3, node);
        }
        return null;
    }
}
