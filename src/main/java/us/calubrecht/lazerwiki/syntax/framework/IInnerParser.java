package us.calubrecht.lazerwiki.syntax.framework;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;

public interface IInnerParser {

    char keyCharacter();

    void setRegistrar(ParserRegistrar registrar);

    Pair<Integer, ITreeNode> parse(String markup, int position);
}
