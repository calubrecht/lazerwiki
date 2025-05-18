package us.calubrecht.lazerwiki.model;

import jakarta.persistence.*;

import java.util.Objects;

@Entity(name = "mediaOverrides")
public class MediaOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String site;
    private String sourcePageNS;
    private String sourcePageName;
    private String targetFileNS;
    private String targetFileName;
    private String newTargetFileNS;
    private String newTargetFileName;

    public MediaOverride() {
    }

    public MediaOverride(String site, String sourcePageNS, String sourcePageName, String targetFileNS, String targetFileName, String newTargetFileNS, String newTargetFileName) {
        this.site = site;
        this.sourcePageNS = sourcePageNS;
        this.sourcePageName = sourcePageName;
        this.targetFileNS = targetFileNS;
        this.targetFileName = targetFileName;
        this.newTargetFileNS = newTargetFileNS;
        this.newTargetFileName = newTargetFileName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getSourcePageNS() {
        return sourcePageNS;
    }

    public void setSourcePageNS(String sourcePageNS) {
        this.sourcePageNS = sourcePageNS;
    }

    public String getSourcePageName() {
        return sourcePageName;
    }

    public void setSourcePageName(String sourcePageName) {
        this.sourcePageName = sourcePageName;
    }

    public String getTargetFileNS() {
        return targetFileNS;
    }

    public void setTargetFileNS(String targetFileNS) {
        this.targetFileNS = targetFileNS;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public void setTargetFileName(String targetFileName) {
        this.targetFileName = targetFileName;
    }

    public String getNewTargetFileNS() {
        return newTargetFileNS;
    }

    public void setNewTargetFileNS(String newTargetFileNS) {
        this.newTargetFileNS = newTargetFileNS;
    }

    public String getNewTargetFileName() {
        return newTargetFileName;
    }

    public void setNewTargetFileName(String newTargetFileName) {
        this.newTargetFileName = newTargetFileName;
    }

    @Transient
    public String getTarget() {
        if (targetFileNS == null || targetFileNS.isBlank()) {
            return targetFileName;
        }
        return targetFileNS + ":" + targetFileName;
    }

    @Transient
    public String getNewTarget() {
        if (newTargetFileNS == null || newTargetFileNS.isBlank()) {
            return newTargetFileName;
        }
        return newTargetFileNS + ":" + newTargetFileName;
    }

    @Transient
    public String getSource() {
        if (sourcePageNS == null || sourcePageNS.isBlank()) {
            return sourcePageName;
        }
        return sourcePageNS + ":" + sourcePageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaOverride that = (MediaOverride) o;
        return Objects.equals(id, that.id) && Objects.equals(site, that.site) && Objects.equals(sourcePageNS, that.sourcePageNS) && Objects.equals(sourcePageName, that.sourcePageName) && Objects.equals(targetFileNS, that.targetFileNS) && Objects.equals(targetFileName, that.targetFileName) && Objects.equals(newTargetFileNS, that.newTargetFileNS) && Objects.equals(newTargetFileName, that.newTargetFileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, site, sourcePageNS, sourcePageName, targetFileNS, targetFileName, newTargetFileNS, newTargetFileName);
    }

    @Override
    public String toString() {
        return "LinkOverride{" +
                "id=" + id +
                ", site='" + site + '\'' +
                ", sourcePageNS='" + sourcePageNS + '\'' +
                ", sourcePageName='" + sourcePageName + '\'' +
                ", targetPageNS='" + targetFileNS + '\'' +
                ", targetPageName='" + targetFileName + '\'' +
                ", newTargetPageNS='" + newTargetFileNS + '\'' +
                ", newTargetPageName='" + newTargetFileName + '\'' +
                '}';
    }
}
