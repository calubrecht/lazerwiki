package us.calubrecht.lazerwiki.syntax.nodes;

public class MacroNode extends AbstractNode {
  final String macroText;
  final String macroFullText;

  public MacroNode(String macroText, String macroFullText) {
    this.macroText = macroText;
    this.macroFullText = macroFullText;
  }

  public String getMacroText() {
    return macroText;
  }

  public String getMacroFullText() {
    return macroFullText;
  }
}
