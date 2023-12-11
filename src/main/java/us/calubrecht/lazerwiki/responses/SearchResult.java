package us.calubrecht.lazerwiki.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

public record SearchResult(String namespace, String pageName, String title,  @JsonInclude(JsonInclude.Include.NON_NULL) String resultLine) {
    @JsonIgnore
    public String getDescriptor() {
        return namespace().isBlank() ? pageName() : namespace() + ":" + pageName();
    }
}
