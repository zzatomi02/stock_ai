package com.noono0.stock.integration.kis.credential.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_kis_credential",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_kis_credential_user_mode", columnNames = {"user_id", "mode"}))
@Getter
@Setter
public class UserKisCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 120)
    private String userId;

    @Column(nullable = false, length = 10)
    private String mode;

    @Column(name = "app_key_enc", nullable = false, columnDefinition = "TEXT")
    private String appKeyEnc;

    @Column(name = "app_secret_enc", nullable = false, columnDefinition = "TEXT")
    private String appSecretEnc;

    @Column(name = "account_no_enc", columnDefinition = "TEXT")
    private String accountNoEnc;

    @Column(name = "product_code", length = 10)
    private String productCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
