package us.calubrecht.lazerwiki.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import us.calubrecht.lazerwiki.model.Namespace;

public class NsNode {
  List<NsNode> children = new ArrayList<>();
  final String namespace;

  final String fullNamespace;

  final boolean writable;

  private Namespace.RestrictionType restrictionType;
  private Namespace.RestrictionType inheritedRestrictionType;

  public NsNode(String namespace, boolean writable) {
    String[] namespaces = namespace.split(":");
    this.namespace = namespaces[namespaces.length - 1];
    this.fullNamespace = namespace;
    this.writable = writable;
  }

  public List<NsNode> getChildren() {
    return new ArrayList<>(children);
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

  public Namespace.RestrictionType getRestrictionType() {
    return restrictionType;
  }

  public void setRestrictionType(Namespace.RestrictionType restrictionType) {
    this.restrictionType = restrictionType;
  }

  public Namespace.RestrictionType getInheritedRestrictionType() {
    return inheritedRestrictionType;
  }

  public void setInheritedRestrictionType(Namespace.RestrictionType inheritedRestrictionType) {
    this.inheritedRestrictionType = inheritedRestrictionType;
  }

  @JsonIgnore
  public Namespace.RestrictionType getRestrictionTypeToPass() {
    if (shouldInherit()) {
      return inheritedRestrictionType;
    }
    return restrictionType;
  }

  @JsonIgnore
  public boolean shouldInherit() {
    return restrictionType == null || restrictionType == Namespace.RestrictionType.INHERIT;
  }
}
