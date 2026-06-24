package us.calubrecht.lazerwiki.model;

import jakarta.persistence.*;

@Entity(name = "tag")
public class PageTag {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne()
  @JoinColumns({
    @JoinColumn(name = "pageId", referencedColumnName = "id"),
    @JoinColumn(name = "revision", referencedColumnName = "revision")
  })
  private Page page;

  private String tag;

  public PageTag() {}

  public PageTag(Page page, String tag) {
    this.page = page;
    this.tag = tag;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Page getPage() {
    return page;
  }

  public void setPage(Page page) {
    this.page = page;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }
}
