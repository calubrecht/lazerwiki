package us.calubrecht.lazerwiki.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.Site;

@Repository
public interface SiteRepository extends CrudRepository<Site, String> {
    Site findByHostname(String hostName);

    Site findBySiteName(String siteName);
}
