package us.calubrecht.lazerwiki.repository;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import us.calubrecht.lazerwiki.model.ImageRef;

public interface ImageRefRepository extends CrudRepository<ImageRef, Long> {
  List<ImageRef> findAllBySiteAndSourcePageNSAndSourcePageName(
      String site, String sourcePageNS, String sourcePageName);

  List<ImageRef> findAllBySiteAndImageNSAndImageRef(String site, String imageNS, String imageRef);

  void deleteBySiteAndSourcePageNSAndSourcePageName(
      String site, String sourcePageNS, String sourcePageName);

  void deleteBySite(String site);
}
