package us.calubrecht.lazerwiki.syntax.parser;

import us.calubrecht.lazerwiki.syntax.framework.IInnerParser;
import us.calubrecht.lazerwiki.syntax.framework.ParserRegistrar;

public abstract class AbstractInnerParser implements IInnerParser {
  ParserRegistrar registrar;

  public void setRegistrar(ParserRegistrar registrar) {
    this.registrar = registrar;
  }
}
