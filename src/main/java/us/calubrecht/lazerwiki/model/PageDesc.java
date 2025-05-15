package us.calubrecht.lazerwiki.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;

import java.time.LocalDateTime;

public interface PageDesc {

    String getNamespace();

    String getPagename();

    Long getRevision();

    String getTitle();

    @JsonIgnore
    String getModifiedByUserName();

    @SuppressWarnings("SameReturnValue")
    LocalDateTime getModified();

    boolean isDeleted();

    @JsonIgnore
    default String getDescriptor() {
        return getNamespace().isBlank() ? getPagename() : getNamespace() + ":" + getPagename();
    }

    @JsonIgnore
    default String getString() {
        return getDescriptor() + "#" + getRevision() + "-" + getModifiedByUserName() + "-" + getModified() + (isDeleted() ? "-Deleted" : "");
    }

    default String getModifiedBy() {
        return getModifiedByUserName();
    }
}
