package us.calubrecht.lazerwiki.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import java.util.Objects;

@Entity(name = "pageCache")
@IdClass(PageCache.PageCacheKey.class)
public class PageCache {

    @Id
    public String site;
    @Id
    public String namespace;
    @Id
    public String pageName;

    public String renderedCache;
    public String plaintextCache;
    public boolean useCache;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageCache pageCache = (PageCache) o;
        return useCache == pageCache.useCache && Objects.equals(site, pageCache.site) && Objects.equals(namespace, pageCache.namespace) && Objects.equals(pageName, pageCache.pageName) && Objects.equals(renderedCache, pageCache.renderedCache) && Objects.equals(plaintextCache, pageCache.plaintextCache);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, namespace, pageName, renderedCache, plaintextCache, useCache);
    }

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
