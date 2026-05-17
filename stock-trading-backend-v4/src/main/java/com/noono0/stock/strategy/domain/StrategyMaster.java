package com.noono0.stock.strategy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "strategy_master")
@Getter
@Setter
public class StrategyMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "strategy_type", nullable = false, unique = true, length = 50)
    private String strategyType;

    @Column(name = "strategy_name", nullable = false, length = 100)
    private String strategyName;

    @Column(length = 500)
    private String description;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
