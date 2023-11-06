package us.calubrecht.lazerwiki.model;

import jakarta.persistence.*;

import java.util.List;

@Entity(name = "userRecord")
public class User {

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
}
