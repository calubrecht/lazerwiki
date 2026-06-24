package us.calubrecht.lazerwiki.syntax.nodes;

public class ControlRowNode extends AbstractNode {
  final String token;

  public ControlRowNode(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }
}
