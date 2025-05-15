package us.calubrecht.lazerwiki.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import us.calubrecht.lazerwiki.model.ActivityLog;

@Repository
public interface ActivityLogRepository extends CrudRepository<ActivityLog, Long> {
}
