package us.calubrecht.lazerwiki.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import us.calubrecht.lazerwiki.model.ActivityLog;
import us.calubrecht.lazerwiki.model.ActivityType;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.repository.ActivityLogRepository;

@SpringBootTest(classes = {ActivityLogService.class})
@ActiveProfiles("test")
class ActivityLogServiceTest {

  @Autowired ActivityLogService service;

  @MockitoBean ActivityLogRepository repo;

  @Test
  void log() {
    User user = new User("Bob", "hash");
    service.log(ActivityType.ACTIVITY_PROTO_CREATE_PAGE, "default", user, "newPage");

    verify(repo)
        .save(new ActivityLog(ActivityType.ACTIVITY_PROTO_CREATE_PAGE, "default", "newPage", user));
  }
}
