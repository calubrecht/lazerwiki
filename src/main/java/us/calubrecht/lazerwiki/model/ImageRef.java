package us.calubrecht.lazerwiki.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Objects;

@Entity(name = "imageRefs")
public class ImageRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String site;
    private String sourcePageNS;
    private String sourcePageName;
    private String imageNS;
    private String imageRef;

    public ImageRef() {

    }

    public ImageRef(String site, String sourcePageNS, String sourcePageName, String imageNS, String imageRef) {
        this.site = site;
        this.sourcePageNS = sourcePageNS;
        this.sourcePageName = sourcePageName;
        this.imageNS = imageNS;
        this.imageRef = imageRef;
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

    public String getImageNS() {
        return imageNS;
    }

    public void setImageNS(String imageNS) {
        this.imageNS = imageNS;
    }

    public String getImageRef() {
        return imageRef;
    }

    public void setImageRef(String imageRef) {
        this.imageRef = imageRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageRef imageRef1 = (ImageRef) o;
        return Objects.equals(id, imageRef1.id) && site.equals(imageRef1.site) && sourcePageNS.equals(imageRef1.sourcePageNS) && sourcePageName.equals(imageRef1.sourcePageName) && imageNS.equals(imageRef1.imageNS) && imageRef.equals(imageRef1.imageRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, site, sourcePageNS, sourcePageName, imageNS, imageRef);
    }

    @Override
    public String toString() {
        return "ImageRef{" +
                "id=" + id +
                ", site='" + site + '\'' +
                ", sourcePageNS='" + sourcePageNS + '\'' +
                ", sourcePageName='" + sourcePageName + '\'' +
                ", imageNS='" + imageNS + '\'' +
                ", imageRef='" + imageRef + '\'' +
                '}';
    }
}
