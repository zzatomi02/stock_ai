package com.noono0.stock.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "password_reset_code",
        indexes = @Index(name = "idx_reset_user_created", columnList = "user_id, created_at"))
@Getter
@Setter
public class PasswordResetCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "verify_attempts", nullable = false)
    private int verifyAttempts;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (verifyAttempts < 0) {
            verifyAttempts = 0;
        }
    }
}
