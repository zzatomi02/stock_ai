package com.noono0.stock.ops.system.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_control")
@Getter
@Setter
public class SystemControl {
    @Id
    private Integer id;

    @Column(name = "emergency_stop", nullable = false)
    private Boolean emergencyStop;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = LocalDateTime.now();
        if (emergencyStop == null) {
            emergencyStop = Boolean.FALSE;
        }
    }
}
