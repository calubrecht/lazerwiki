package us.calubrecht.lazerwiki.repository;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.LinkOverride;

@Repository
public interface LinkOverrideRepository extends CrudRepository<LinkOverride, Long> {

  List<LinkOverride> findAllBySiteAndSourcePageNSAndSourcePageNameOrderById(
      String site, String sourcePageNS, String sourcePageName);

  List<LinkOverride> findAllBySiteAndTargetPageNSAndTargetPageName(
      String any, String eq, String pageName);

  List<LinkOverride> findAllBySiteAndNewTargetPageNSAndNewTargetPageName(
      String any, String eq, String pageName);

  void deleteBySiteAndSourcePageNSAndSourcePageName(
      String site, String sourcePageNS, String sourcePageName);

  void deleteBySiteAndNewTargetPageNSAndNewTargetPageName(
      String site, String newTargetPageNS, String newTargetPageName);
}
