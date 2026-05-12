package us.calubrecht.lazerwiki.syntax.nodes;

public class HeaderNode extends ContainerNode{
    int level;
    String content;

    public HeaderNode(int level) {
        this.level = level;
        this.content = content;
    }

    public int getLevel() {
        return level;
    }

}
