package us.calubrecht.lazerwiki.model;

import java.time.LocalDateTime;
import java.util.List;

public record RecentChangesResponse(List<RecentChangeRec> changes, List<MediaHistoryRecord> mediaChanges, List<Object> merged) {

    public record RecentChangeRec(PageDesc pageDesc, String action)
    {

    }

    public static RecentChangeRec recFor(PageDesc pageDesc) {
        String action;
        if (pageDesc.isDeleted()) {
            action = "Deleted";
        }
        else if (pageDesc.getRevision() == 1L) {
            action = "Created";
        }
        else {
            action = "Modified";
        }
        return new RecentChangeRec(pageDesc, action);
    }
}
