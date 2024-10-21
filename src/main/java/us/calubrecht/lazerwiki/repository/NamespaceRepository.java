package us.calubrecht.lazerwiki.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import us.calubrecht.lazerwiki.model.Namespace;

import java.util.Set;

public interface NamespaceRepository extends CrudRepository<Namespace, Long> {

    @Cacheable("FindBySiteAndNamespace")
    Namespace findBySiteAndNamespace(String site, String namespace);

    @Query(value="SELECT namespace FROM known_namespaces where site = :site", nativeQuery = true)
    Set<String> getKnownNamespaces(@Param("site") String site);

    void deleteBySite(String site);
}
