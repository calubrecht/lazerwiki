package us.calubrecht.lazerwiki.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity(name = "sites")
public class Site {

    @Id
    public String name;

    public String hostname;

    public String siteName;

    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> settings;
}
