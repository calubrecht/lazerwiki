package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.TextNode;
import us.calubrecht.lazerwiki.syntax.nodes.UnformatSpanNode;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UnformatSpanParser extends AbstractInnerParser{
    final Pattern unformatPattern = Pattern.compile("^%%(.*?)%%", Pattern.DOTALL);

    @Override
    public Collection<Character> keyCharacters() {
        return List.of('%');
    }

    @Override
    public Pair<Integer, ITreeNode> parse(String markup, int position) {
        Matcher m = unformatPattern.matcher(markup);

        if (m.find()) {
            UnformatSpanNode node = new UnformatSpanNode();
            int length = m.group(0).length();
            node.setPosition(Pair.of(position, position + length - 1));
            TextNode innerNode = new TextNode(m.group(1));
            innerNode.setPosition(Pair.of(position +2, position +2 + m.group(1).length() -1));
            node.addChild(innerNode);
            return Pair.of(length, node);
        }
        return null;
    }
}