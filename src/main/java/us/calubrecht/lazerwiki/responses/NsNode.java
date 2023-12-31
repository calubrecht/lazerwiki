package us.calubrecht.lazerwiki.responses;

import java.util.ArrayList;
import java.util.List;

public class NsNode {
    List<NsNode> children = new ArrayList<>();
    final String namespace;

    final String fullNamespace;

    final boolean writable;

    public NsNode(String namespace, boolean writable) {
        String[] namespaces = namespace.split(":");
        this.namespace = namespaces[namespaces.length-1];
        this.fullNamespace = namespace;
        this.writable = writable;
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

    public boolean isWritable() {
        return writable;
    }
}
