package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.BoldNode;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BoldParser extends AbstractInnerParser {
    final Pattern boldPattern = Pattern.compile("^\\*\\*(.*?)\\*\\*", Pattern.DOTALL);
    @Override
    public char keyCharacter() {
        return '*';
    }

    @Override
    public Pair<Integer, ITreeNode> parse(String markup, int position) {
        Matcher m = boldPattern.matcher(markup);

        if (m.find()) {
            BoldNode boldNode = new BoldNode();
            int length = m.group(0).length();
            boldNode.setPosition(Pair.of(position, position + length - 1));
            Parser.parseInner(List.of(m.group(1)), boldNode, position+2, registrar);
            return Pair.of(length, boldNode);
        }
        return null;
    }
}
