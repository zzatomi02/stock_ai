package com.noono0.stock.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "auth_email_request_log",
        indexes = @Index(name = "idx_auth_email_req", columnList = "email, request_type, created_at"))
@Getter
@Setter
public class AuthEmailRequestLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 32)
    private AuthEmailRequestType requestType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
