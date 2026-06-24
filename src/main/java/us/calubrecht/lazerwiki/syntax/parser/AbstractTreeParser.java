package us.calubrecht.lazerwiki.syntax.parser;

import us.calubrecht.lazerwiki.syntax.framework.ITreeParser;
import us.calubrecht.lazerwiki.syntax.framework.ParserRegistrar;

public abstract class AbstractTreeParser implements ITreeParser {
  ParserRegistrar registrar;

  public void setRegistrar(ParserRegistrar registrar) {
    this.registrar = registrar;
  }
}
