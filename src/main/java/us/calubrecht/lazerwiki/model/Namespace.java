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

  public String site;
  public String namespace;

  @Enumerated(EnumType.STRING)
  @Column(name = "restriction_type")
  public RestrictionType restrictionType;
}
