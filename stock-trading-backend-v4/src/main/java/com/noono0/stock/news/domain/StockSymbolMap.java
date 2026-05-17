package com.noono0.stock.news.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_symbol_map")
@Getter
@Setter
public class StockSymbolMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String kisCode;

    private String stockName;
    private String naverQuery;
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
