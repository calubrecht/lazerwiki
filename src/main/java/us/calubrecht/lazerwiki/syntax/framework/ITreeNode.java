package us.calubrecht.lazerwiki.syntax.framework;

import org.apache.commons.lang3.tuple.Pair;

public interface ITreeNode {
  void setPosition(Pair<Integer, Integer> position);

  Pair<Integer, Integer> getPosition();

  String asString();

  /** For validation and debugging reference */
  void setParseContext(ReadOnlyParseContext parseContext);

  ReadOnlyParseContext getParseContext();

  default void setPosition(int start, int end) {
    setPosition(Pair.of(start, end));
  }

  default String getSourceFromContext() {
    if (getParseContext() == null || getPosition() == null) {
      // Node has not been fully initialized, cannot get source
      return null;
    }
    ReadOnlyParseContext context = getParseContext();
    Pair<Integer, Integer> position = getPosition();
    // Position uses inclusive endpoints, substring's end is exclusive
    return context.getFullText().substring(position.getLeft(), position.getRight() + 1);
  }
}
