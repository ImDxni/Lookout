package me.dani.lookout.auth.jpa;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

import static me.dani.lookout.shared.utils.Normalizer.normalizeEmail;

@Entity
@Table(name = "app_user")
@Getter
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false,
            name = "password_hash")
    private String password;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AppUser(){}

    public AppUser(String email, String password){
        this.email = normalizeEmail(email);
        this.password = password;
    }
}
