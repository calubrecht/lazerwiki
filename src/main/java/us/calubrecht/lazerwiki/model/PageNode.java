package us.calubrecht.lazerwiki.model;

import java.util.ArrayList;
import java.util.List;

public class PageNode {
    List<PageNode> children = new ArrayList<>();
    String ns;

    public PageNode(String ns) {
        String[] namespaces = ns.split(":");
        this.ns = namespaces[namespaces.length-1];
    }

    public PageNode() {

    }

    public List<PageNode> getChildren() {
        return new ArrayList<PageNode>(children);
    }

    public void setChildren(List<PageNode> children) {
        this.children = children;
    }

    public String getNamespace() {
        return ns;
    }

    public static class TerminalNode extends PageNode {
        PageDesc p;

        public TerminalNode(PageDesc p) {
            this.p = p;
        }

        public PageDesc getPage() {
            return p;
        }
    }
}
