package us.calubrecht.lazerwiki.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import us.calubrecht.lazerwiki.service.UserService;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "mediaHistory")
public class MediaHistoryRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String site;

    private String namespace;

    @ManyToOne()
    @JoinColumn(name="uploadedBy", referencedColumnName = "userId")
    @JsonIgnore
    public User uploadedByUser;

    @ManyToOne()
    @JoinColumn(name="action", referencedColumnName = "activityTypeId")
    @JsonIgnore
    private ActivityType activity;

    @CreationTimestamp
    private LocalDateTime ts;

    public MediaHistoryRecord() {
    }

    public MediaHistoryRecord(String fileName, String site, String namespace, User uploadedBy, ActivityType activity) {
        this.fileName = fileName;
        this.site = site;
        this.namespace = namespace;
        this.uploadedByUser = uploadedBy;
        this.activity = activity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public User getUploadedByUser() {
        return uploadedByUser;
    }

    public void setUploadedByUser(User uploadedByUser) {
        this.uploadedByUser = uploadedByUser;
    }

    public String getUploadedBy() { return uploadedByUser == null ? UserService.MISSING_USER : uploadedByUser.userName;}

    public String getAction() {
        return activity.simpleName;
    }

    public LocalDateTime getTs() {
        return ts;
    }

    public void setTs(LocalDateTime ts) {
        this.ts = ts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaHistoryRecord that = (MediaHistoryRecord) o;
        return Objects.equals(id, that.id) && Objects.equals(fileName, that.fileName) && Objects.equals(site, that.site) && Objects.equals(namespace, that.namespace) && Objects.equals(uploadedByUser, that.uploadedByUser) && Objects.equals(activity, that.activity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fileName, site, namespace, uploadedByUser, activity);
    }

    @Override
    public String toString() {
        return "MediaHistoryRecord{" +
                "fileName='" + fileName + '\'' +
                ", site='" + site + '\'' +
                ", namespace='" + namespace + '\'' +
                ", uploadedBy='" + uploadedByUser + '\'' +
                ", action='" + activity + '\'' +
                ", ts=" + ts +
                '}';
    }
}
