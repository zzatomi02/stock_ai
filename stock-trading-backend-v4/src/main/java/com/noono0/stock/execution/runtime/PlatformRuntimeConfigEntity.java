package com.noono0.stock.execution.runtime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_runtime_config")
@Getter
@Setter
public class PlatformRuntimeConfigEntity {
    @Id
    @Column(name = "config_key", length = 64)
    private String configKey;

    @Column(name = "config_json", nullable = false, columnDefinition = "JSON")
    private String configJson;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
