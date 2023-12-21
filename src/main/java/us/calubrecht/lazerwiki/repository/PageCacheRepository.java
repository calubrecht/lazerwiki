package us.calubrecht.lazerwiki.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.PageCache;
import us.calubrecht.lazerwiki.model.PageDesc;

import java.util.List;

@Repository
public interface PageCacheRepository extends CrudRepository<PageCache, PageCache.PageCacheKey> {

    void deleteBySite(String site);

    @Query(value = "SELECT * FROM pageCache WHERE MATCH(pageName, title) AGAINST (:searchTerm IN BOOLEAN MODE) AND site=:site", nativeQuery= true)
    List<PageCache> searchByTitle(String site, String searchTerm);

    @Query(value = "SELECT * FROM pageCache WHERE MATCH(plaintextCache) AGAINST (:searchTerm IN BOOLEAN MODE) AND site=:site", nativeQuery = true)
    List<PageCache> searchByText(String site, String searchTerm);
}
