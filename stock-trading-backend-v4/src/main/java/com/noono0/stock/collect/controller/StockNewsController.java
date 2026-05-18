package com.noono0.stock.collect.controller;

import com.noono0.stock.collect.domain.StockNews;
import com.noono0.stock.collect.repository.StockNewsRepository;
import com.noono0.stock.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collect/news")
@RequiredArgsConstructor
public class StockNewsController {
    private final StockNewsRepository stockNewsRepository;

    @GetMapping
    public ApiResponse<List<StockNews>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(
                stockNewsRepository
                        .findAll(PageRequest.of(page, Math.min(size, 200), Sort.by(Sort.Direction.DESC, "collectedAt")))
                        .getContent());
    }

    @GetMapping("/{id}")
    public ApiResponse<StockNews> detail(@PathVariable long id) {
        return ApiResponse.ok(
                stockNewsRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("뉴스 없음: " + id)));
    }
}
