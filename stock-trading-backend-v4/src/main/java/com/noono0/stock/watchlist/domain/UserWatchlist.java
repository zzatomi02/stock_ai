package com.noono0.stock.watchlist.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_watchlist",
        uniqueConstraints = @UniqueConstraint(name = "uk_watchlist_user_stock", columnNames = {"user_id", "stock_code"}))
@Getter
@Setter
public class UserWatchlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "stock_name", length = 120)
    private String stockName;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
