package us.calubrecht.lazerwiki.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.swing.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "activityLog")
public class ActivityLog {

    public ActivityLog(ActivityType activityType, String target, String user) {
        this.activityType = activityType;
        this.target = target;
        this.user = user;
    }

    public ActivityLog() {

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "activityTypeId")
    public ActivityType activityType;

    @Column
    String target;

    @Column
    String user;

    @CreationTimestamp
    LocalDateTime timestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}