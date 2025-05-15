package us.calubrecht.lazerwiki.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "activityType")
public class ActivityType {
    public static ActivityType ACTIVITY_PROTO_UPLOAD_MEDIA = new ActivityType(50L);
    public static ActivityType ACTIVITY_PROTO_REPLACE_MEDIA = new ActivityType(60L);
    public static ActivityType ACTIVITY_PROTO_DELETE_MEDIA = new ActivityType(70L);

    public ActivityType() {

    }

    public ActivityType(Long activityTypeId) {
        this.activityTypeId = activityTypeId;
    }

    public ActivityType(Long activityTypeId, String activityName, String simpleName, String fullDesc) {
        this.activityTypeId = activityTypeId;
        this.activityName = activityName;
        this.simpleName = simpleName;
        this.fullDesc = fullDesc;
    }

    @Id
    Long activityTypeId;

    @Column
    String activityName;

    @Column
    String simpleName;

    @Column
    String fullDesc;
}
