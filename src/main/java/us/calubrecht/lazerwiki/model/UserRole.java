package us.calubrecht.lazerwiki.model;

import jakarta.persistence.*;

@Entity(name = "userRole")
public class UserRole {

    public UserRole() {}

    public UserRole(User user, String role) {
        this.user = user;
        this.role = role;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne()
    @JoinColumn(name="userName", referencedColumnName = "userName")
    public User user;

    @Column(nullable = false)
    public String role;
}
