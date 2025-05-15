package us.calubrecht.lazerwiki.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "pageLock")
@IdClass(PageLockKey.class)
public class PageLock {
    public PageLock() {
    }

    public PageLock(String site, String namespace, String pagename, User owner, LocalDateTime lockTime, String lockId) {
        this.site = site;
        this.namespace = namespace;
        this.pagename = pagename;
        this.owner = owner;
        this.lockTime = lockTime;
        this.lockId = lockId;
    }

    @Id
    private String site;
    @Id
    private String namespace;
    @Id
    private String pagename;

    @ManyToOne
    @JoinColumn(name="owner", referencedColumnName = "userId")
    private User owner;

    private LocalDateTime lockTime;

    private String lockId;

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

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public LocalDateTime getLockTime() {
        return lockTime;
    }

    public void setLockTime(LocalDateTime lockTime) {
        this.lockTime = lockTime;
    }

    public String getLockId() {
        return lockId;
    }

    public void setLockId(String lockId) {
        this.lockId = lockId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageLock pageLock = (PageLock) o;
        return Objects.equals(site, pageLock.site) && Objects.equals(namespace, pageLock.namespace) && Objects.equals(pagename, pageLock.pagename) && Objects.equals(owner, pageLock.owner) && Objects.equals(lockTime, pageLock.lockTime) && Objects.equals(lockId, pageLock.lockId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, namespace, pagename, owner, lockTime, lockId);
    }

    @Override
    public String toString() {
        return "PageLock{" +
                "site='" + site + '\'' +
                ", namespace='" + namespace + '\'' +
                ", pagename='" + pagename + '\'' +
                ", owner='" + owner + '\'' +
                ", lockTime=" + lockTime +
                ", lockId='" + lockId + '\'' +
                '}';
    }
}
