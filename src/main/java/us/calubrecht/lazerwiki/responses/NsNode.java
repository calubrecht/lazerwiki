package us.calubrecht.lazerwiki.responses;

import us.calubrecht.lazerwiki.model.Namespace;

import java.util.ArrayList;
import java.util.List;

public class NsNode {
    List<NsNode> children = new ArrayList<>();
    final String namespace;

    final String fullNamespace;

    final boolean writable;

    private Namespace.RESTRICTION_TYPE restriction_type;

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

    public Namespace.RESTRICTION_TYPE getRestriction_type() {
        return restriction_type;
    }

    public void setRestriction_type(Namespace.RESTRICTION_TYPE restriction_type) {
        this.restriction_type = restriction_type;
    }
}
