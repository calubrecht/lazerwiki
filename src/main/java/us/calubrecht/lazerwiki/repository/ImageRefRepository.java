package us.calubrecht.lazerwiki.repository;

import org.springframework.data.repository.CrudRepository;
import us.calubrecht.lazerwiki.model.ImageRef;

import java.util.List;

public interface ImageRefRepository extends CrudRepository<ImageRef, Long> {
    List<ImageRef> findAllBySiteAndSourcePageNSAndSourcePageName(String site, String sourcePageNS, String sourcePageName);
    List<ImageRef> findAllBySiteAndImageNSAndImageRef(String site, String imageNS, String imageRef);
    void deleteBySiteAndSourcePageNSAndSourcePageName(String site, String sourcePageNS, String sourcePageName);
}
