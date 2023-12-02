package us.calubrecht.lazerwiki.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Objects;

@Entity(name = "links")
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String site;
    private String sourcePageNS;
    private String sourcePageName;
    private String targetPageNS;
    private String targetPageName;

    public Link() {
    }

    public Link(String site, String sourcePageNS, String sourcePageName, String targetPageNS, String targetPageName) {
        this.site = site;
        this.sourcePageNS = sourcePageNS;
        this.sourcePageName = sourcePageName;
        this.targetPageNS = targetPageNS;
        this.targetPageName = targetPageName;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link = (Link) o;
        return Objects.equals(id, link.id) && site.equals(link.site) && sourcePageNS.equals(link.sourcePageNS) && sourcePageName.equals(link.sourcePageName) && targetPageNS.equals(link.targetPageNS) && targetPageName.equals(link.targetPageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, site, sourcePageNS, sourcePageName, targetPageNS, targetPageName);
    }

    @Override
    public String toString() {
        return "Link{" +
                "site='" + site + '\'' +
                ", sourcePageNS='" + sourcePageNS + '\'' +
                ", sourcePageName='" + sourcePageName + '\'' +
                ", targetPageNS='" + targetPageNS + '\'' +
                ", targetPageName='" + targetPageName + '\'' +
                '}';
    }
}
