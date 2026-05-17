package com.noono0.stock.backtest;

import com.noono0.stock.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 백테스트·리플레이: KIS 현재가 참조 간이 시뮬레이션 (추후 과거 캔들·체결 로그 연동) */
@RestController
@RequestMapping("/api/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestService backtestService;

    @GetMapping("/replay")
    public ApiResponse<?> replay(
            @RequestParam(name = "stockCode") String stockCode,
            @RequestParam(name = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(backtestService.replay(stockCode, from, to));
    }
}
