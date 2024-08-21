package us.calubrecht.lazerwiki.repository;

import org.springframework.data.repository.CrudRepository;
import us.calubrecht.lazerwiki.model.PageLock;
import us.calubrecht.lazerwiki.model.PageLockKey;

public interface PageLockRepository  extends CrudRepository<PageLock, PageLockKey> {
    PageLock findBySiteAndNamespaceAndPagename(String site, String namespace, String pagename);

    void deleteBySiteAndNamespaceAndPagenameAndLockId(String site, String namespace, String pagename, String lockId);

    void deleteBySiteAndNamespaceAndPagename(String site, String namespace, String pagename);
}
