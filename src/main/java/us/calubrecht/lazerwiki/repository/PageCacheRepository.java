package us.calubrecht.lazerwiki.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.PageCache;

@Repository
public interface PageCacheRepository extends CrudRepository<PageCache, PageCache.PageCacheKey> {

    void deleteBySite(String site);
}
