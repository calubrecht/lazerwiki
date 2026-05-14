package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.LinkNode;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LinkParser extends AbstractInnerParser {
    final Pattern linkPattern = Pattern.compile("^\\[\\[( *[0-9A-z:_\\-/.]* *)(\\|([^\\]]*))?\\]\\]");
    @Override
    public List<Character> keyCharacters() {
        return List.of('[');
    }

    @Override
    public Pair<Integer, ITreeNode> parse(String markup, int start) {
        // Like takes the form [[linkPath|Link Description]]
        Matcher matcher = linkPattern.matcher(markup);
        if (matcher.find()) {
            String dest = matcher.group(1);
            String desc = matcher.group(3);
            int length = matcher.group().length();
            LinkNode node = new LinkNode(dest);
            node.setPosition(Pair.of(start, start + length - 1));
            node.setTargetPosition(Pair.of(start + 2, start + 2 + dest.length() - 1));
            if (desc != null) {
                Parser.parseInner(List.of(desc), node, start+2, registrar);
            }
            return Pair.of(length, node);
        }
        return null;
    }
}
