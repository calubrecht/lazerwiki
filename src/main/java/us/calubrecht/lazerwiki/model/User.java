package us.calubrecht.lazerwiki.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity(name = "userRecord")
public class User {
    public static final String GUEST = "Guest";

    public User() {}
    public User(String userName, String passwordHash) {
        this.userName = userName;
        this.passwordHash = passwordHash;
    }
    @Id
    @Column(name = "userId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int userId;

    @Column(name = "userName")
    public String userName;

    @Column(name = "passwordHash")
    public String passwordHash;

    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> settings;

    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "userId")
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userName, user.userName) && Objects.equals(passwordHash, user.passwordHash) && Objects.equals(settings, user.settings) && Objects.equals(roles, user.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, passwordHash, settings, roles);
    }
}
