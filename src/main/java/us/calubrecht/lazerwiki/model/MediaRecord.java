package us.calubrecht.lazerwiki.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Objects;

@Entity(name = "mediaRecord")
public class MediaRecord {

    public MediaRecord() {

    }

    public MediaRecord(String fileName, String site, String uploadedBy, long fileSize, int height, int width) {
        this.fileName = fileName;
        this.site = site;
        this.uploadedBy = uploadedBy;
        this.fileSize = fileSize;
        this.height = height;
        this.width = width;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String site;

    private String uploadedBy;

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
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaRecord that = (MediaRecord) o;
        return fileSize == that.fileSize && height == that.height && width == that.width && id.equals(that.id) && fileName.equals(that.fileName) && site.equals(that.site) && uploadedBy.equals(that.uploadedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fileName, site, uploadedBy, fileSize, height, width);
    }

    @Override
    public String toString() {
        return "MediaRecord{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", site='" + site + '\'' +
                ", uploadedBy='" + uploadedBy + '\'' +
                ", fileSize=" + fileSize +
                ", height=" + height +
                ", width=" + width +
                '}';
    }
}