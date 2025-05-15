package us.calubrecht.lazerwiki.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.ActivityLog;
import us.calubrecht.lazerwiki.model.ActivityType;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.repository.ActivityLogRepository;

@Service
public class ActivityLogService {

    @Autowired
    ActivityLogRepository repo;

    public void log(ActivityType activity, User user, String target) {
        repo.save(new ActivityLog(activity, target, user));
    }
}
