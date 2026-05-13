package us.calubrecht.lazerwiki.syntax.framework;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;

public interface IInnerParser {

    Collection<Character> keyCharacters();

    void setRegistrar(ParserRegistrar registrar);

    Pair<Integer, ITreeNode> parse(String markup, int position);
}
