package us.calubrecht.lazerwiki.model;

import jakarta.persistence.*;

import java.util.Objects;

@Entity(name = "linkOverrides")
public class LinkOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String site;
    private String sourcePageNS;
    private String sourcePageName;
    private String targetPageNS;
    private String targetPageName;
    private String newTargetPageNS;
    private String newTargetPageName;

    public LinkOverride() {
    }

    public LinkOverride(String site, String sourcePageNS, String sourcePageName, String targetPageNS, String targetPageName, String newTargetPageNS, String newTargetPageName) {
        this.site = site;
        this.sourcePageNS = sourcePageNS;
        this.sourcePageName = sourcePageName;
        this.targetPageNS = targetPageNS;
        this.targetPageName = targetPageName;
        this.newTargetPageNS = newTargetPageNS;
        this.newTargetPageName = newTargetPageName;
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

    public String getTargetPageNS() {
        return targetPageNS;
    }

    public void setTargetPageNS(String targetPageNS) {
        this.targetPageNS = targetPageNS;
    }

    public String getTargetPageName() {
        return targetPageName;
    }

    public void setTargetPageName(String targetPageName) {
        this.targetPageName = targetPageName;
    }

    public String getNewTargetPageNS() {
        return newTargetPageNS;
    }

    public void setNewTargetPageNS(String newTargetPageNS) {
        this.newTargetPageNS = newTargetPageNS;
    }

    public String getNewTargetPageName() {
        return newTargetPageName;
    }

    public void setNewTargetPageName(String newTargetPageName) {
        this.newTargetPageName = newTargetPageName;
    }

    @Transient
    public String getTarget() {
        if (targetPageNS == null || targetPageNS.isBlank()) {
            return targetPageName;
        }
        return targetPageNS + ":" + targetPageName;
    }

    @Transient
    public String getNewTarget() {
        if (newTargetPageNS == null || newTargetPageNS.isBlank()) {
            return newTargetPageName;
        }
        return newTargetPageNS + ":" + newTargetPageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkOverride that = (LinkOverride) o;
        return Objects.equals(id, that.id) && Objects.equals(site, that.site) && Objects.equals(sourcePageNS, that.sourcePageNS) && Objects.equals(sourcePageName, that.sourcePageName) && Objects.equals(targetPageNS, that.targetPageNS) && Objects.equals(targetPageName, that.targetPageName) && Objects.equals(newTargetPageNS, that.newTargetPageNS) && Objects.equals(newTargetPageName, that.newTargetPageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, site, sourcePageNS, sourcePageName, targetPageNS, targetPageName, newTargetPageNS, newTargetPageName);
    }

    @Override
    public String toString() {
        return "LinkOverride{" +
                "id=" + id +
                ", site='" + site + '\'' +
                ", sourcePageNS='" + sourcePageNS + '\'' +
                ", sourcePageName='" + sourcePageName + '\'' +
                ", targetPageNS='" + targetPageNS + '\'' +
                ", targetPageName='" + targetPageName + '\'' +
                ", newTargetPageNS='" + newTargetPageNS + '\'' +
                ", newTargetPageName='" + newTargetPageName + '\'' +
                '}';
    }
}
