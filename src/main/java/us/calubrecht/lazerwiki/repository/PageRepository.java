package us.calubrecht.lazerwiki.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.model.PageKey;
import us.calubrecht.lazerwiki.model.PageText;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PageRepository extends CrudRepository<Page, PageKey> {

    Page findBySiteAndNamespaceAndPagenameAndValidtsAndDeleted(String site, String namespace, String pagename, LocalDateTime validts, boolean deleted);
    <T> T findBySiteAndNamespaceAndPagenameAndValidts(String site, String namespace, String pagename, LocalDateTime validts, Class<T> type);

    Page findBySiteAndNamespaceAndPagenameAndRevision(String site, String namespace, String pagename, long revision);
    List<PageDesc> findAllBySiteAndValidtsAndDeletedOrderByModifiedDesc(String site, LocalDateTime validts, boolean deleted);
    List<PageDesc> findAllBySiteAndNamespaceInOrderByModifiedDesc(Limit limit, String site, List<String> namespaces);
    List<PageDesc> findAllBySiteAndNamespaceAndPagenameOrderByRevision(String site, String namespace, String pagename);

    @Query(value="SELECT namespace, pagename, title, modifiedBy, modified FROM page p inner join tag t on p.id= t.pageId and p.revision = t.revision  where p.site = :site and t.tag=:tagName and deleted=0 and validTS='9999-12-31 00:00:00'",
            nativeQuery = true)
    List<PageDesc> getByTagname(@Param("site") String site, @Param("tagName") String tagName);

    @Query(value="SELECT distinct namespace FROM page where site=:site")
    List<String> getAllNamespaces(@Param("site") String site);

    LocalDateTime MAX_DATE = LocalDateTime.of(9999, 12, 31, 0, 0, 0);
    default Page getBySiteAndNamespaceAndPagenameAndDeleted(String site, String namespace, String pagename, boolean deleted)
    {
        return findBySiteAndNamespaceAndPagenameAndValidtsAndDeleted(site, namespace, pagename, MAX_DATE, deleted);
    }

    default Page getBySiteAndNamespaceAndPagename(String site, String namespace, String pagename)
    {
        return findBySiteAndNamespaceAndPagenameAndValidts(site, namespace, pagename, MAX_DATE, Page.class);
    }

    default Long getLastRevisionBySiteAndNamespaceAndPagename(String site, String namespace, String pagename)
    {
        PageDesc desc =  findBySiteAndNamespaceAndPagenameAndValidts(site, namespace, pagename, MAX_DATE, PageDesc.class);
        return desc == null ? null : desc.getRevision();
    }

    default List<PageDesc> getAllValid(String site)
    {
        return findAllBySiteAndValidtsAndDeletedOrderByModifiedDesc(site, MAX_DATE, false);
    }

    @Query(value="SELECT namespace, pagename, title, text, deleted FROM page WHERE site=:site AND deleted=0 and validTS='9999-12-31 00:00:00' AND concat(namespace , ':' , pagename) IN (:pageDescs)",
            nativeQuery = true)
    List<PageText> getAllBySiteAndNamespaceAndPagename(String site, List<String> pageDescs);

    void deleteBySite(String site);
}
