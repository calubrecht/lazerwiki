package us.calubrecht.lazerwiki.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.MediaRecord;

import java.util.List;

@Repository
public interface MediaRecordRepository extends CrudRepository<MediaRecord, Long> {

    List<MediaRecord> findAllBySiteOrderByFileName(String site);
}
