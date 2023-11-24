package us.calubrecht.lazerwiki.requests;

import java.util.List;

public class SavePageRequest {
    private String text;
    private List<String> tags;

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
}
