package us.calubrecht.lazerwiki.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.model.PageKey;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PageRepository extends CrudRepository<Page, PageKey> {

    Page findBySiteAndNamespaceAndPagenameAndValidtsAndDeleted(String site, String namespace, String pagename, LocalDateTime validts, boolean deleted);
    Page findBySiteAndNamespaceAndPagenameAndValidts(String site, String namespace, String pagename, LocalDateTime validts);
    List<PageDesc> findAllBySiteAndValidtsAndDeletedOrderByModifiedDesc(String site, LocalDateTime validts, boolean deleted);
    List<PageDesc> findAllBySiteAndNamespaceAndPagenameOrderByRevision(String site, String namespace, String pagename);

    @Query(value="SELECT namespace, pagename, title, modifiedBy, modified FROM page p inner join tag t on p.id= t.pageId and p.revision = t.revision  where p.site = :site and t.tag=:tagName and deleted=0 and validTS='9999-12-31 00:00:00'",
            nativeQuery = true)
    List<PageDesc> getByTagname(@Param("site") String site, @Param("tagName") String tagName);

    LocalDateTime MAX_DATE = LocalDateTime.of(9999, 12, 31, 0, 0, 0);
    default Page getBySiteAndNamespaceAndPagenameAndDeleted(String site, String namespace, String pagename, boolean deleted)
    {
        return findBySiteAndNamespaceAndPagenameAndValidtsAndDeleted(site, namespace, pagename, MAX_DATE, deleted);
    }

    default Page getBySiteAndNamespaceAndPagename(String site, String namespace, String pagename)
    {
        return findBySiteAndNamespaceAndPagenameAndValidts(site, namespace, pagename, MAX_DATE);
    }

    default List<PageDesc> getAllValid(String site)
    {
        return findAllBySiteAndValidtsAndDeletedOrderByModifiedDesc(site, MAX_DATE, false);
    }

}
