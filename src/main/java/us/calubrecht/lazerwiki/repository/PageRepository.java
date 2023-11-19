package us.calubrecht.lazerwiki.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.model.PageKey;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PageRepository extends CrudRepository<Page, PageKey> {

    Page findBySiteAndNamespaceAndPagenameAndValidtsAndDeleted(String site, String namespace, String pagename, LocalDateTime validts, boolean deleted);
    List<PageDesc> findAllBySiteAndValidtsAndDeleted(String site, LocalDateTime validts, boolean deleted);

    static final LocalDateTime MAX_DATE = LocalDateTime.of(9999, 12, 31, 0, 0, 0);
    default Page getBySiteAndNamespaceAndPagenameAndDeleted(String site, String namespace, String pagename, boolean deleted)
    {
        return findBySiteAndNamespaceAndPagenameAndValidtsAndDeleted(site, namespace, pagename, MAX_DATE, deleted);
    }

    default List<PageDesc> getAllValid(String site)
    {
        return findAllBySiteAndValidtsAndDeleted(site, MAX_DATE, false);
    }


}
