package com.noono0.stock.news.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.news.domain.StockSymbolMap;
import com.noono0.stock.news.mapper.StockSymbolMapMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stock-map")
@RequiredArgsConstructor
public class StockSymbolMapController {
    private final StockSymbolMapMapper stockSymbolMapMapper;

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(stockSymbolMapMapper.findAll());
    }

    @PostMapping
    public ApiResponse<?> upsert(@RequestBody List<StockSymbolMap> rows) {
        for (StockSymbolMap row : rows) {
            if (row.getCreatedAt() == null) {
                row.setCreatedAt(LocalDateTime.now());
            }
            stockSymbolMapMapper.upsert(row);
        }
        return ApiResponse.ok(stockSymbolMapMapper.findAll());
    }
}
