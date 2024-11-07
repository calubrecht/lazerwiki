package us.calubrecht.lazerwiki.requests;

import java.util.List;

public class SavePageRequest {
    private String text;
    private List<String> tags;
    private int revision;
    private Boolean force;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public int getRevision() {return revision;}

    public void setRevision(int revision) {this.revision = revision;}

    public Boolean isForce() {return force;}

    public void setForce(boolean force) {this.force = force;}
}
