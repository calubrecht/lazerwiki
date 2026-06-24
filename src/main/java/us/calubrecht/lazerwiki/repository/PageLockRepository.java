package us.calubrecht.lazerwiki.repository;

import org.springframework.data.repository.CrudRepository;
import us.calubrecht.lazerwiki.model.PageLock;
import us.calubrecht.lazerwiki.model.PageLockKey;
import us.calubrecht.lazerwiki.model.User;

public interface PageLockRepository  extends CrudRepository<PageLock, PageLockKey> {
    PageLock findBySiteAndNamespaceAndPagename(String site, String namespace, String pagename);

    void deleteBySiteAndNamespaceAndPagenameAndLockId(String site, String namespace, String pagename, String lockId);

    void deleteBySiteAndNamespaceAndPagenameAndLockIdAndOwner(String site, String namespace, String pagename, String lockId, User owner);

    void deleteBySiteAndNamespaceAndPagename(String site, String namespace, String pagename);

    void deleteBySite(String site);
}
