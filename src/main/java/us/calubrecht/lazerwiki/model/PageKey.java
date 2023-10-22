package us.calubrecht.lazerwiki.model;

import java.util.Objects;

public class PageKey {
    private Long id;
    private Long revision;

    public PageKey(Long id, Long revision) {
        this.id = id;
        this.revision = revision;
    }

    public PageKey() {}

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageKey pageKey = (PageKey) o;
        return Objects.equals(id, pageKey.id) && Objects.equals(revision, pageKey.revision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, revision);
    }

}
