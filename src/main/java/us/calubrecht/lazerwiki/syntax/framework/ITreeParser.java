package us.calubrecht.lazerwiki.syntax.framework;

public interface ITreeParser {
  /**
   * Parse the markupLines into an ITreeNode, or null if Parser cannot parse this input; If lines
   * are parsed, call advanceLine on parseContext
   */
  ITreeNode parse(ParseContext parseContext);

  void setRegistrar(ParserRegistrar registrar);

  /**
   * Parser analyzes the next line to do a quick short-circuit analysis to tell if it can parse the
   * block
   */
  boolean canBeginParse(String line);

  String parserKey();

  default int priority() {
    return 100;
  }
}
