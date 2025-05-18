package us.calubrecht.lazerwiki.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.MediaOverride;

import java.util.List;

@Repository
public interface MediaOverrideRepository extends CrudRepository<MediaOverride, Long> {

    List<MediaOverride> findAllBySiteAndSourcePageNSAndSourcePageNameOrderById(String site, String sourcePageNS, String sourcePageName);
    List<MediaOverride> findAllBySiteAndTargetFileNSAndTargetFileName(String any, String ns, String pageName);
    List<MediaOverride> findAllBySiteAndNewTargetFileNSAndNewTargetFileName(String any, String ns, String fileName);
    void deleteBySiteAndSourcePageNSAndSourcePageName(String site, String sourcePageNS, String sourcePageName);
    void deleteBySiteAndNewTargetFileNSAndNewTargetFileName(String site, String newTargetFileNS, String newTargetFileName);

    void deleteBySite(String site);

}
