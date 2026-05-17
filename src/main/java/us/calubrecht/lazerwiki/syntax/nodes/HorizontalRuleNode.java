package us.calubrecht.lazerwiki.syntax.nodes;

public class HorizontalRuleNode extends AbstractNode{
    final String src;

    public HorizontalRuleNode(String src) {
        this.src = src;
    }

    public String getSource() {
        return src;
    }
}
