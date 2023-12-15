package us.calubrecht.lazerwiki.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "pageCache")
@IdClass(PageCache.PageCacheKey.class)
public class PageCache implements PageDesc {

    public PageCache() {

    }

    public PageCache(String site, String namespace, String pageName, String title, String renderedCache, String plaintextCache,  boolean useCache) {
        this.site = site;
        this.namespace = namespace;
        this.pageName = pageName;
        this.renderedCache = renderedCache;
        this.plaintextCache = plaintextCache;
        this.title = title;
        this.useCache = useCache;
    }

    @Id
    public String site;
    @Id
    public String namespace;
    @Id
    public String pageName;

    public String renderedCache;
    public String plaintextCache;

    public String title;
    public boolean useCache;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageCache pageCache = (PageCache) o;
        return useCache == pageCache.useCache && Objects.equals(site, pageCache.site) && Objects.equals(namespace, pageCache.namespace) && Objects.equals(pageName, pageCache.pageName) && Objects.equals(renderedCache, pageCache.renderedCache) && Objects.equals(plaintextCache, pageCache.plaintextCache) && Objects.equals(title, pageCache.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, namespace, pageName, renderedCache, plaintextCache, useCache, title);
    }

    @JsonIgnore
    public PageDesc toDesc() {
        return new PageDesc() {

            @Override
            public String getNamespace() {
                return namespace;
            }

            @Override
            public String getPagename() {
                return pageName;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public String getModifiedBy() {
                return null;
            }

            @Override
            public LocalDateTime getModified() {
                return null;
            }

            @Override
            public boolean isDeleted() {
                return false;
            }

            @Override
            public Long getRevision() { return null;}
        };
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    @JsonIgnore
    public String getPagename() {
        return pageName;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    @JsonIgnore
    public String getModifiedBy() {
        return null;
    }

    @Override
    @JsonIgnore
    public LocalDateTime getModified() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isDeleted() {
        return false;
    }

    @Override
    @JsonIgnore
    public Long getRevision() { return null;}

    public static class PageCacheKey {
        public String site;
        public String namespace;
        public String pageName;

        public PageCacheKey() {

        }

        public PageCacheKey(String site, String namespace, String pageName) {
            this.site = site;
            this.namespace = namespace;
            this.pageName = pageName;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PageCacheKey that = (PageCacheKey) o;
            return site.equals(that.site) && namespace.equals(that.namespace) && pageName.equals(that.pageName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(site, namespace, pageName);
        }

        @Override
        public String toString() {
            return "PageCacheKey{" +
                    "site='" + site + '\'' +
                    ", namespace='" + namespace + '\'' +
                    ", pageName='" + pageName + '\'' +
                    '}';
        }
    }
}
