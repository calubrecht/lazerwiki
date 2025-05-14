package us.calubrecht.lazerwiki.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "activityLog")
public class ActivityType {

    public ActivityType() {

    }

    public ActivityType(Long id, String activityName) {
        this.id = id;
        this.activityName = activityName;
    }

    @Id
    Long id;

    @Column
    String activityName;
}
