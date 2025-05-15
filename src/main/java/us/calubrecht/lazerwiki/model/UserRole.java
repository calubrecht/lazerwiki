package us.calubrecht.lazerwiki.model;

import jakarta.persistence.*;

import java.util.Objects;

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
    @JoinColumn(name="userId", referencedColumnName = "userId")
    public User user;

    @Column(nullable = false)
    public String role;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserRole userRole = (UserRole) o;
        return Objects.equals(id, userRole.id) && Objects.equals(role, userRole.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, role);
    }
}
