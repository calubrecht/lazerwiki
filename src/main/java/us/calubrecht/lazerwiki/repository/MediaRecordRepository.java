package us.calubrecht.lazerwiki.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.MediaRecord;

import java.util.List;

@Repository
public interface MediaRecordRepository extends CrudRepository<MediaRecord, Long> {

    List<MediaRecord> findAllBySiteOrderByFileName(String site);

    MediaRecord findBySiteAndNamespaceAndFileName(String site, String namespace, String fileName);

    @Modifying
    @Query("DELETE FROM mediaRecord WHERE fileName = :fileName and site= :site and namespace= :namespace")
    void deleteBySiteAndFilenameAndNamespace(@Param("site") String site, @Param("fileName") String fileName, @Param("namespace") String namespace);

    @Query(value="SELECT distinct namespace FROM mediaRecord where site=:site")
    List<String> getAllNamespaces(@Param("site") String site);

    void deleteBySite(String site);
}
