package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.LineBreakNode;

import java.util.Collection;
import java.util.List;

@Component
public class LineBreakParser extends AbstractInnerParser {
    @Override
    public Collection<Character> keyCharacters() {
        return List.of(' ');
    }

    @Override
    public Pair<Integer, ITreeNode> parse(String markup, int position) {
        if (markup.startsWith(" \\\\")) {
            LineBreakNode node = new LineBreakNode();
            node.setPosition(Pair.of(position, position+2));
            return Pair.of(3, node);
        }
        return null;
    }
}
