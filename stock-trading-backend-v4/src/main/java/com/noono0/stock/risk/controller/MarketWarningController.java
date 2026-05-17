package com.noono0.stock.risk.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.risk.domain.MarketWarningHistory;
import com.noono0.stock.risk.domain.StockWarningStatus;
import com.noono0.stock.risk.dto.StockWarningUpsertRequest;
import com.noono0.stock.risk.dto.WarningFilterResult;
import com.noono0.stock.risk.repository.MarketWarningHistoryRepository;
import com.noono0.stock.risk.service.MarketWarningAdminService;
import com.noono0.stock.risk.service.MarketWarningFilter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market-warnings")
@RequiredArgsConstructor
public class MarketWarningController {

    private final MarketWarningAdminService adminService;
    private final MarketWarningFilter warningFilter;
    private final MarketWarningHistoryRepository historyRepository;

    @GetMapping
    public ApiResponse<List<StockWarningStatus>> list() {
        return ApiResponse.ok(adminService.listActive());
    }

    @GetMapping("/check/{stockCode}")
    public ApiResponse<WarningFilterResult> check(@PathVariable String stockCode) {
        return ApiResponse.ok(warningFilter.evaluate(stockCode));
    }

    @GetMapping("/history/{stockCode}")
    public ApiResponse<List<MarketWarningHistory>> history(@PathVariable String stockCode) {
        return ApiResponse.ok(historyRepository.findTop100ByStockCodeOrderByRecordedAtDesc(stockCode));
    }

    @PutMapping
    public ApiResponse<StockWarningStatus> upsert(@Valid @RequestBody StockWarningUpsertRequest req) {
        return ApiResponse.ok(adminService.upsert(req));
    }

    @DeleteMapping("/{stockCode}")
    public ApiResponse<?> deactivate(@PathVariable String stockCode) {
        adminService.deactivate(stockCode);
        return ApiResponse.ok(null, "경보 해제");
    }
}
