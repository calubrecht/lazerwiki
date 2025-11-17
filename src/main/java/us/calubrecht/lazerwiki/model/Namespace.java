package us.calubrecht.lazerwiki.model;

import jakarta.persistence.*;

@Entity(name = "namespace")
public class Namespace {
  public enum RESTRICTION_TYPE { OPEN, WRITE_RESTRICTED, READ_RESTRICTED, GUEST_WRITABLE, INHERIT}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;
  public String site;
  public String namespace;

  @Enumerated(EnumType.STRING)
  public RESTRICTION_TYPE restriction_type;

}
