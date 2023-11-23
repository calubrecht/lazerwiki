package us.calubrecht.lazerwiki.model;

import jakarta.persistence.*;
import java.util.List;

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

    @OneToMany(cascade = {CascadeType.ALL})
    @JoinColumn(name = "userName")
    public List<UserRole> roles; // Should roles be site specific?

    public static boolean isGuest(String userName) {
        return userName == null || userName.equals(GUEST);
    }
}
