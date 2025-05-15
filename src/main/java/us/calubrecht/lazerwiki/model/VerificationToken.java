package us.calubrecht.lazerwiki.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "verificationToken")
public class VerificationToken {
    public enum Purpose {VERIFY_EMAIL, RESET_PASSWORD};

    public VerificationToken() {

    }

    public VerificationToken(User user, String token, Purpose purpose, String data) {
        this.user = user;
        this.token = token;
        this.purpose = purpose;
        this.data = data;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name="user", referencedColumnName = "userId")
    private User user;
    private String token;
    private String data;

    @Column(columnDefinition = "ENUM('VERIFY_EMAIL', 'RESET_PASSWORD')")
    @Enumerated(EnumType.STRING)
    private Purpose purpose;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public void setPurpose(Purpose purpose) {
        this.purpose = purpose;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VerificationToken that = (VerificationToken) o;
        return Objects.equals(user, that.user) && Objects.equals(token, that.token) && Objects.equals(data, that.data) && purpose == that.purpose;
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, token, data, purpose);
    }
}
