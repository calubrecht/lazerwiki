package us.calubrecht.lazerwiki.syntax.framework;

import java.util.Collection;
import org.apache.commons.lang3.tuple.Pair;

public interface IInnerParser {

  Collection<Character> keyCharacters();

  void setRegistrar(ParserRegistrar registrar);

  Pair<Integer, ITreeNode> parse(ParseContext parseContext);
}
