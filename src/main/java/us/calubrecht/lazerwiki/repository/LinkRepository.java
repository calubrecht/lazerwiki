package us.calubrecht.lazerwiki.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.Link;
import us.calubrecht.lazerwiki.model.MediaRecord;

import java.util.List;

@Repository
public interface LinkRepository extends CrudRepository<Link, Long> {

    public List<Link> findAllBySiteAndSourcePageNSAndSourcePageName(String site, String sourcePageNS, String sourcePageName);
    public List<Link> findAllBySiteAndTargetPageNSAndTargetPageName(String site, String targetPageNS, String targetPageName);
    public void deleteBySiteAndSourcePageNSAndSourcePageName(String site, String sourcePageNS, String sourcePageName);
}
