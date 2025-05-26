package us.calubrecht.lazerwiki.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import us.calubrecht.lazerwiki.service.UserService;

import java.util.Objects;

@Entity(name = "mediaRecord")
public class MediaRecord {

    public MediaRecord() {

    }

    public MediaRecord(String fileName, String site, String namespace, User uploadedBy, long fileSize, int height, int width) {
        this.fileName = fileName;
        this.site = site;
        this.namespace = namespace;
        this.uploadedByUser = uploadedBy;
        this.fileSize = fileSize;
        this.height = height;
        this.width = width;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String site;

    private String namespace;

    @ManyToOne
    @JoinColumn(name="uploadedBy", referencedColumnName = "userId")
    @JsonIgnore
    private User uploadedByUser;

    private long fileSize;

    private int height;

    private int width;

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

    public String getUploadedBy() {
        return uploadedByUser == null ? UserService.MISSING_USER : uploadedByUser.userName;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedByUser = uploadedBy;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaRecord that = (MediaRecord) o;
        return fileSize == that.fileSize && height == that.height && width == that.width && Objects.equals(id, that.id) && Objects.equals(fileName, that.fileName) && Objects.equals(site, that.site) && Objects.equals(namespace, that.namespace) && Objects.equals(uploadedByUser, that.uploadedByUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fileName, site, namespace, uploadedByUser, fileSize, height, width);
    }

    @Override
    public String toString() {
        return "MediaRecord{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", site='" + site + '\'' +
                ", namespace='" + namespace + '\'' +
                ", uploadedBy='" + uploadedByUser + '\'' +
                ", fileSize=" + fileSize +
                ", height=" + height +
                ", width=" + width +
                '}';
    }
}