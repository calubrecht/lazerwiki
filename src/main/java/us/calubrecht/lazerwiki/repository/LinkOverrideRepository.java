package us.calubrecht.lazerwiki.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.LinkOverride;

import java.util.List;

@Repository
public interface LinkOverrideRepository extends CrudRepository<LinkOverride, Long> {

    List<LinkOverride> findAllBySiteAndSourcePageNSAndSourcePageNameOrderById(String site, String sourcePageNS, String sourcePageName);
    List<LinkOverride> findAllBySiteAndNewTargetPageNSAndNewTargetPageName(String any, String eq, String pageName);
    void deleteBySiteAndSourcePageNSAndSourcePageName(String site, String sourcePageNS, String sourcePageName);
    void deleteBySiteAndNewTargetPageNSAndNewTargetPageName(String site, String newTargetPageNS, String newTargetPageName);

    void deleteBySite(String site);

}
