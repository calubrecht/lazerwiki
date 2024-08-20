package us.calubrecht.lazerwiki.responses;

import java.time.LocalDateTime;

public record PageLockResponse(String namespace, String pagename, Long revision, String owner, LocalDateTime lockTime, Boolean success) {

}
