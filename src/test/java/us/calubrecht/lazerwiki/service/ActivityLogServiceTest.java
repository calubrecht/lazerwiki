package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.ActivityLog;
import us.calubrecht.lazerwiki.model.ActivityType;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.repository.ActivityLogRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {ActivityLogService.class})
@ActiveProfiles("test")
class ActivityLogServiceTest {

    @Autowired
    ActivityLogService service;

    @MockBean
    ActivityLogRepository repo;

    @Test
    void log() {
        User user = new User("Bob", "hash");
        service.log(ActivityType.ACTIVITY_PROTO_CREATE_PAGE, user, "newPage");

        verify(repo).save(new ActivityLog(ActivityType.ACTIVITY_PROTO_CREATE_PAGE, "newPage", user));
    }
}