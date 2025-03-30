package us.calubrecht.lazerwiki.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity(name = "verificationToken")
public class VerificationToken {
    public enum Purpose {VERIFY_EMAIL, RESET_PASSWORD};

    public VerificationToken() {

    }

    public VerificationToken(String user, String token, Purpose purpose) {
        this.user = user;
        this.token = token;
        this.purpose = purpose;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String user;
    private String token;

    @Column(columnDefinition = "ENUM('VERIFY_EMAIL', 'RESET_PASSWORD')")
    @Enumerated(EnumType.STRING)
    private Purpose purpose;

    //@GeneratedValue(strategy = GenerationType.AUTO)
//    private LocalDateTime expiry;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
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

/*
    public LocalDateTime getExpiry() {
        return expiry;
    }

    public void setExpiry(LocalDateTime expiry) {
        this.expiry = expiry;
    }*/
}
