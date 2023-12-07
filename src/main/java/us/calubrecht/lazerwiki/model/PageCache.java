package us.calubrecht.lazerwiki.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

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
    }
}
