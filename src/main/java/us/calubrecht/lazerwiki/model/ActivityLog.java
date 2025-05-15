package us.calubrecht.lazerwiki.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.swing.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity(name = "activityLog")
public class ActivityLog {

    public ActivityLog(ActivityType activityType, String target, User user) {
        this.activityType = activityType;
        this.target = target;
        this.user = user;
    }

    public ActivityLog() {

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @JoinColumn(name = "activityType", referencedColumnName = "activityTypeId")
    public ActivityType activityType;

    @Column
    String target;

    @ManyToOne
    @JoinColumn(name="user", referencedColumnName = "userId")
    @JsonIgnore
    private User user;

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ActivityLog that = (ActivityLog) o;
        return Objects.equals(id, that.id) && Objects.equals(activityType, that.activityType) && Objects.equals(target, that.target) && Objects.equals(user, that.user) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, activityType, target, user, timestamp);
    }
}