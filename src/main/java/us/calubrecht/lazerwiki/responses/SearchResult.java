package us.calubrecht.lazerwiki.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record SearchResult(String namespace, String pageName, String title, String resultLine) {
    @JsonIgnore
    public String getDescriptor() {
        return namespace().isBlank() ? pageName() : namespace() + ":" + pageName();
    }
}
