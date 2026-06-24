package us.calubrecht.lazerwiki.model;

import jakarta.persistence.*;

@SuppressWarnings("unused")
@Entity(name = "namespace")
public class Namespace {
  public enum RestrictionType {
    OPEN,
    WRITE_RESTRICTED,
    READ_RESTRICTED,
    GUEST_WRITABLE,
    INHERIT
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  private String site;
  private String namespace;

  @Enumerated(EnumType.STRING)
  @Column(name = "restriction_type")
  public RestrictionType restrictionType;

  public String getSite() {
    return site;
  }

  public void setSite(String site) {
    this.site = site;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }
}
