package com.edu.unimag.rodatours.security.reset;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "password_resets")
@Getter @Setter
public class PasswordReset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 255) private String email;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(length = 512) private String encryptedToken;
    @Column(nullable = false) private Instant requestedAt;
    @Column(nullable = false) private Instant expiresAt;
    private Instant consumedAt;
    private Instant sentAt;
    @Column(nullable = false) private int attempts;
    @Column(nullable = false) private Instant nextAttemptAt;
    @Version private Long version;
}
