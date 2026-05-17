package com.noono0.stock.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "app_user",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_app_user_username", columnNames = "username"),
            @UniqueConstraint(name = "uk_app_user_email", columnNames = "email")
        })
@Getter
@Setter
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(length = 255)
    private String email;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
