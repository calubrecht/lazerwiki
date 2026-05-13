package us.calubrecht.lazerwiki.syntax.nodes;

public class LinkNode extends ContainerNode{
    String dest;

    public LinkNode(String dest) {
        this.dest = dest;
    }

    public String getDest() {
        return dest;
    }

}