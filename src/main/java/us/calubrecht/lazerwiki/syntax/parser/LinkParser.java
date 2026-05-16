package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
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
    public Pair<Integer, ITreeNode> parse(ParseContext parseContext) {
        // Like takes the form [[linkPath|Link Description]]
        CharSequence sequence = parseContext.subsequence();
        int start = parseContext.getPosition();
        Matcher matcher = linkPattern.matcher(sequence);
        if (matcher.find()) {
            String dest = matcher.group(1);
            String desc = matcher.group(3);
            int length = matcher.group().length();
            LinkNode node = new LinkNode(dest);
            node.setPosition(Pair.of(start, start + length - 1));
            node.setTargetPosition(Pair.of(start + 2, start + 2 + dest.length() - 1));
            node.setParseContext(parseContext);
            if (!Strings.isBlank(desc)) {
                ParseContext descContext = new ParseContext(desc, start + 3 + dest.length(), parseContext);
                Parser.parseInner(descContext, node, registrar);
            }
            return Pair.of(length, node);
        }
        return null;
    }
}
