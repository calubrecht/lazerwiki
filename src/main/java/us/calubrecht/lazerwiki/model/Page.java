package us.calubrecht.lazerwiki.model;


import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "page")
@IdClass(PageKey.class)
public class Page {
    @Id
    @Column(name="id")
    private Long id;

    @Id
    @Column(name="revision")
    private Long revision;
    String text;

    String site;
    String namespace;

    String pagename;
    String title;

    String modifiedBy;

    @CreationTimestamp
    LocalDateTime modified;

    @Column(name = "validTS")
    LocalDateTime validts;
    boolean deleted;

    @OneToMany(cascade = {CascadeType.ALL})
    @JoinColumns({
      @JoinColumn(name = "pageId", referencedColumnName="id"),
      @JoinColumn(name = "revision", referencedColumnName="revision")}
    )
    public List<PageTag> tags;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

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

    public String getPagename() {
        return pagename;
    }

    public void setPagename(String pagename) {
        this.pagename = pagename;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }


    public LocalDateTime getValidts() {
        return validts;
    }

    public void setValidts(LocalDateTime validts) {
        this.validts = validts;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public LocalDateTime getModified() {
        return modified;
    }

    public void setModified(LocalDateTime modified) {
        this.modified = modified;
    }

    public List<PageTag> getTags() {
        return tags;
    }

    public void setTags(List<PageTag> tags) {
        this.tags = tags;
    }
}
