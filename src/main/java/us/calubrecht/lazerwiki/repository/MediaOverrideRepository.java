package us.calubrecht.lazerwiki.repository;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.MediaOverride;

@Repository
public interface MediaOverrideRepository extends CrudRepository<MediaOverride, Long> {

  List<MediaOverride> findAllBySiteAndSourcePageNSAndSourcePageNameOrderById(
      String site, String sourcePageNS, String sourcePageName);

  List<MediaOverride> findAllBySiteAndNewTargetFileNSAndNewTargetFileName(
      String any, String ns, String fileName);

  void deleteBySiteAndSourcePageNSAndSourcePageName(
      String site, String sourcePageNS, String sourcePageName);

  void deleteBySiteAndNewTargetFileNSAndNewTargetFileName(
      String site, String newTargetFileNS, String newTargetFileName);
}
