package us.calubrecht.lazerwiki.syntax.parser;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.nodes.TextNode;
import us.calubrecht.lazerwiki.syntax.nodes.UnformatSpanNode;

@Component
public class UnformatSpanParser extends AbstractInnerParser {
  final Pattern unformatPattern = Pattern.compile("^%%(.*?)%%", Pattern.DOTALL);
  final Pattern nowikiPattern = Pattern.compile("^<nowiki>(.*?)</nowiki>", Pattern.DOTALL);

  @Override
  public Collection<Character> keyCharacters() {
    return List.of('%', '<');
  }

  @Override
  public Pair<Integer, ITreeNode> parse(ParseContext parseContext) {
    CharSequence sequence = parseContext.subsequence();
    int position = parseContext.getPosition();
    Matcher m1 = unformatPattern.matcher(sequence);
    Matcher m2 = nowikiPattern.matcher(sequence);

    if (m1.find() || m2.find()) {
      Matcher m = m1.hasMatch() ? m1 : m2;
      UnformatSpanNode node = new UnformatSpanNode();
      int length = m.group(0).length();
      node.setPosition(Pair.of(position, position + length - 1));
      node.setParseContext(parseContext);
      TextNode innerNode = new TextNode(m.group(1));
      innerNode.setPosition(Pair.of(position + m.start(1), position + m.end(1) - 1));
      innerNode.setParseContext(parseContext);
      node.addChild(innerNode);
      return Pair.of(length, node);
    }
    return null;
  }
}
