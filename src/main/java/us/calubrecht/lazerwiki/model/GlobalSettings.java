package us.calubrecht.lazerwiki.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Entity(name = "globalSettings")
public class GlobalSettings {
    public static final String ENABLE_SELF_REG = "enableSelfReg";

    public GlobalSettings() {
       settings = new HashMap<>();
    }

    @Id
    public Integer id;

    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> settings;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GlobalSettings that = (GlobalSettings) o;
        return Objects.equals(id, that.id) && Objects.equals(settings, that.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, settings);
    }

    @Override
    public String toString() {
        return "GlobalSettings{" +
                "id=" + id +
                ", settings=" + settings +
                '}';
    }
}
