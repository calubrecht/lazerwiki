package us.calubrecht.lazerwiki.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;

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

    private String uploadedBy;

    private String action;

    @CreationTimestamp
    private LocalDateTime ts;

    public MediaHistoryRecord() {
    }

    public MediaHistoryRecord(String fileName, String site, String namespace, String uploadedBy, String action) {
        this.fileName = fileName;
        this.site = site;
        this.namespace = namespace;
        this.uploadedBy = uploadedBy;
        this.action = action;
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

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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
        return Objects.equals(id, that.id) && Objects.equals(fileName, that.fileName) && Objects.equals(site, that.site) && Objects.equals(namespace, that.namespace) && Objects.equals(uploadedBy, that.uploadedBy) && Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fileName, site, namespace, uploadedBy, action);
    }

    @Override
    public String toString() {
        return "MediaHistoryRecord{" +
                "fileName='" + fileName + '\'' +
                ", site='" + site + '\'' +
                ", namespace='" + namespace + '\'' +
                ", uploadedBy='" + uploadedBy + '\'' +
                ", action='" + action + '\'' +
                ", ts=" + ts +
                '}';
    }
}
