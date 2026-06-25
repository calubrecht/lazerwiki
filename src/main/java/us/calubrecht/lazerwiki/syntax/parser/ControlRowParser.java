package us.calubrecht.lazerwiki.syntax.parser;

import java.util.Set;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.nodes.ControlRowNode;

@Component
public class ControlRowParser extends AbstractTreeParser {
  static final Set<String> TOKENS = Set.of("~~NOTOC~~", "~~YESTOC~~");

  @Override
  public ITreeNode parse(ParseContext parseContext) {
    String token = parseContext.peekLine().strip();
    int start = parseContext.getPosition() + parseContext.peekLine().indexOf(token);
    parseContext.advanceLine();
    ITreeNode node = new ControlRowNode(token);
    node.setParseContext(parseContext);
    node.setPosition(start, start + token.length() - 1);
    return node;
  }

  @Override
  public boolean canBeginParse(String line) {
    return TOKENS.contains(line.strip());
  }

  @Override
  public String parserKey() {
    return "ControlRow";
  }
}
