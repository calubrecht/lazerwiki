package us.calubrecht.lazerwiki.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.MediaHistoryRecord;

import java.util.List;

@Repository
public interface MediaHistoryRepository  extends CrudRepository<MediaHistoryRecord, Long> {

    @Query(value="SELECT distinct namespace FROM mediaHistory where site=:site")
    List<String> getAllNamespaces(@Param("site") String site);

    List<MediaHistoryRecord> findAllBySiteAndNamespaceInOrderByTsDesc(Limit limit, String site, List<String> namespaces);
}
