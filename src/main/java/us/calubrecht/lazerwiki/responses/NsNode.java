package us.calubrecht.lazerwiki.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import us.calubrecht.lazerwiki.model.Namespace;

import java.util.ArrayList;
import java.util.List;

public class NsNode {
    List<NsNode> children = new ArrayList<>();
    final String namespace;

    final String fullNamespace;

    final boolean writable;

    private Namespace.RESTRICTION_TYPE restriction_type;
    private Namespace.RESTRICTION_TYPE inherited_restriction_type;

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

    public Namespace.RESTRICTION_TYPE getInherited_restriction_type() {
        return inherited_restriction_type;
    }

    public void setInherited_restriction_type(Namespace.RESTRICTION_TYPE inherited_restriction_type) {
        this.inherited_restriction_type = inherited_restriction_type;
    }

    @JsonIgnore
    public Namespace.RESTRICTION_TYPE getRestrictionTypeToPass() {
        if (shouldInherit()) {
            return inherited_restriction_type;
        }
        return restriction_type;
    }

    @JsonIgnore
    public boolean shouldInherit() {
        return restriction_type == null || restriction_type == Namespace.RESTRICTION_TYPE.INHERIT;
    }
}
