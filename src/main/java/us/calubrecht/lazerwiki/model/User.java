package us.calubrecht.lazerwiki.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity(name = "userRecord")
public class User {
    public static final String GUEST = "Guest";

    public User() {}
    public User(String userName, String passwordHash) {
        this.userName = userName;
        this.passwordHash = passwordHash;
    }
    @Id
    @Column(name = "userName")
    public String userName;

    @Column(name = "passwordHash")
    public String passwordHash;

    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> settings;

    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "userName")
    public List<UserRole> roles; // Should roles be site specific?

    @JsonIgnore
    public List<String> getRolesString() {
        return roles.stream().map(user -> user.role).toList();
    }

    public static boolean isGuest(String userName) {
        return userName == null || userName.equals(GUEST);
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }
}
