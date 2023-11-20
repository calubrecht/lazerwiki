package us.calubrecht.lazerwiki.model;

import java.util.ArrayList;
import java.util.List;

public class NsNode {
    List<NsNode> children = new ArrayList<>();
    String namespace;

    String fullNamespace;

    public NsNode(String namespace) {
        String[] namespaces = namespace.split(":");
        this.namespace = namespaces[namespaces.length-1];
        this.fullNamespace = namespace;
    }

    public NsNode() {

    }

    public List<NsNode> getChildren() {
        return new ArrayList<NsNode>(children);
    }

    public void setChildren(List<NsNode> children) {
        this.children = children;
    }

    public String getNamespace() {
        return namespace;
    }
    public String getFullNamespace() {
        return fullNamespace;
    }
}
