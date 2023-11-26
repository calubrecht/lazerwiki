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

    String getTitle();

    String getModifiedBy();

    LocalDateTime getModified();

    @JsonIgnore
    default public String getDescriptor() {
        return getNamespace().isBlank() ? getPagename() : getNamespace() + ":" + getPagename();
    }
}
