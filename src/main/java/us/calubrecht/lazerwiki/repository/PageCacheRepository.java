package us.calubrecht.lazerwiki.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.PageCache;
import us.calubrecht.lazerwiki.model.PageDesc;

import java.util.List;

@Repository
public interface PageCacheRepository extends CrudRepository<PageCache, PageCache.PageCacheKey> {

    void deleteBySite(String site);

    void deleteBySiteAndNamespaceAndPageName(String site, String namespace, String pageName);

    @Query(value = "SELECT * FROM pageCache WHERE MATCH(pageName, title) AGAINST (:searchTerm IN BOOLEAN MODE) AND site=:site", nativeQuery= true)
    List<PageCache> searchByTitleMysql(String site, String searchTerm);

    @Query(value = "SELECT * FROM pageCache WHERE MATCH(plaintextCache) AGAINST (:searchTerm IN BOOLEAN MODE) AND site=:site", nativeQuery = true)
    List<PageCache> searchByTextMysql(String site, String searchTerm);

    @Query(value = "SELECT * FROM pageCache WHERE title MATCH :searchTerm AND site=:site", nativeQuery= true)
    List<PageCache> searchByTitleSqlite(String site, String searchTerm);

    @Query(value = "SELECT * FROM pageCache WHERE plaintextCache MATCH :searchTerm AND site=:site", nativeQuery = true)
    List<PageCache> searchByTextSqlite(String site, String searchTerm);

    default List<PageCache> searchByTitle(String engine, String site, String searchTerm) {
        if (engine.equals("sqlite")) {
            return searchByTitleSqlite(site, searchTerm);
        }
        return searchByTitleMysql(site, searchTerm);
    }

    default List<PageCache> searchByText(String engine, String site, String searchTerm) {
        if (engine.equals("sqlite")) {
            return searchByTextSqlite(site, searchTerm);
        }
        return searchByTextMysql(site, searchTerm);
    }
}
