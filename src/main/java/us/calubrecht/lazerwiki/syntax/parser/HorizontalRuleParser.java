package us.calubrecht.lazerwiki.syntax.parser;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.nodes.HorizontalRuleNode;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class HorizontalRuleParser extends AbstractTreeParser {
    final Pattern hrPattern = Pattern.compile("^-{4,}$");

    @Override
    public ITreeNode parse(ParseContext parseContext) {
        String src = parseContext.peekLine();
        int start = parseContext.getPosition();
        parseContext.advanceLine();
        ITreeNode node = new HorizontalRuleNode(src);
        node.setParseContext(parseContext);
        node.setPosition(start, start + src.length() - 1);
        return node;
    }

    @Override
    public boolean canBeginParse(String line) {
        return hrPattern.matcher(line).matches();
    }

    @Override
    public String parserKey() {
        return "ControlRow";
    }
}
