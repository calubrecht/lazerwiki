package us.calubrecht.lazerwiki.syntax.nodes;

import org.apache.commons.lang3.tuple.Pair;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ReadOnlyParseContext;

public abstract class AbstractNode implements ITreeNode {
  Pair<Integer, Integer> position = null;
  ReadOnlyParseContext fullContext = null;

  @Override
  public void setPosition(Pair<Integer, Integer> position) {
    this.position = position;
  }

  @Override
  public Pair<Integer, Integer> getPosition() {
    return position;
  }

  @Override
  public void setParseContext(ReadOnlyParseContext parseContext) {
    this.fullContext = parseContext.getRootContext();
  }

  @Override
  public ReadOnlyParseContext getParseContext() {
    return fullContext;
  }

  @Override
  public String asString() {
    throw new UnsupportedOperationException("No default text representation");
  }
}
