package us.calubrecht.lazerwiki.repository;

import org.springframework.data.repository.CrudRepository;
import us.calubrecht.lazerwiki.model.PageLock;
import us.calubrecht.lazerwiki.model.PageLockKey;

public interface PageLockRepository  extends CrudRepository<PageLock, PageLockKey> {
    PageLock findBySiteAndNamespaceAndPagename(String site, String namespace, String pagename);
}
