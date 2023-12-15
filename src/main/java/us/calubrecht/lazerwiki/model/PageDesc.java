package us.calubrecht.lazerwiki.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import java.time.LocalDateTime;

public interface PageDesc {

    String getNamespace();

    String getPagename();

    Long getRevision();

    String getTitle();

    String getModifiedBy();

    @SuppressWarnings("SameReturnValue")
    LocalDateTime getModified();

    boolean isDeleted();

    @JsonIgnore
    default String getDescriptor() {
        return getNamespace().isBlank() ? getPagename() : getNamespace() + ":" + getPagename();
    }
}
