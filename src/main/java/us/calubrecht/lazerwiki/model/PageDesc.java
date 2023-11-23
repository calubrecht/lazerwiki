package us.calubrecht.lazerwiki.model;

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
}
