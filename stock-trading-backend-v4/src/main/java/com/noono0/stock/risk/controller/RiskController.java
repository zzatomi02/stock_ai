package com.noono0.stock.risk.controller;



import com.noono0.stock.common.api.ApiResponse;

import com.noono0.stock.integration.kis.service.KisApiHealthService;

import com.noono0.stock.risk.config.RiskProperties;

import com.noono0.stock.risk.domain.OrderValidationLog;

import com.noono0.stock.risk.domain.RiskEvent;

import com.noono0.stock.risk.dto.RiskStatusResponse;

import com.noono0.stock.risk.repository.OrderValidationLogRepository;

import com.noono0.stock.risk.repository.RiskEventRepository;

import com.noono0.stock.risk.service.RiskDailyStateService;

import com.noono0.stock.risk.service.RiskStrategyDailyService;

import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;

import org.springframework.data.domain.Sort;

import org.springframework.web.bind.annotation.*;



import java.math.BigDecimal;

import java.util.List;

import java.util.Map;



@RestController

@RequestMapping("/api/risk")

@RequiredArgsConstructor

public class RiskController {



    private final OrderValidationLogRepository validationLogRepository;

    private final RiskProperties riskProperties;

    private final RiskDailyStateService dailyStateService;

    private final RiskStrategyDailyService strategyDailyService;

    private final RiskEventRepository eventRepository;

    private final KisApiHealthService apiHealthService;



    @GetMapping("/validation-logs")

    public ApiResponse<List<OrderValidationLog>> logs() {

        return ApiResponse.ok(

                validationLogRepository

                        .findAll(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt")))

                        .getContent());

    }



    @GetMapping("/status")

    public ApiResponse<RiskStatusResponse> status(

            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        var daily = dailyStateService.getOrCreateToday(userId);

        return ApiResponse.ok(

                new RiskStatusResponse(

                        daily.isBuyHalted(),

                        daily.isGlobalHalt(),

                        daily.getHaltReason(),

                        daily.getBuyOrderCount(),

                        daily.getSellOrderCount(),

                        daily.getRealizedPnl(),

                        dailyStateService.dailyLossRatePercent(daily),

                        riskProperties.getDailyMaxLossRatePercent(),

                        riskProperties.getDailyMaxBuyCount(),

                        apiHealthService.isApiHealthy(),

                        apiHealthService.quoteDelayMs(),

                        riskProperties));

    }



    @GetMapping("/events")

    public ApiResponse<List<RiskEvent>> events() {

        return ApiResponse.ok(eventRepository.findTop50ByOrderByCreatedAtDesc());

    }



    @PostMapping("/pnl")

    public ApiResponse<?> recordPnl(

            @RequestHeader(value = "X-User-Id", required = false) String userId,

            @RequestParam(name = "delta") BigDecimal delta) {

        return ApiResponse.ok(dailyStateService.recordRealizedPnl(userId, delta));

    }



    @PostMapping("/stop-loss")

    public ApiResponse<?> recordStopLoss(@RequestParam @NotBlank String strategyType) {

        strategyDailyService.recordStopLoss(strategyType);

        return ApiResponse.ok(Map.of("strategyType", strategyType, "recorded", true));

    }



    @PostMapping("/api-health/recover")

    public ApiResponse<?> recoverApi() {

        apiHealthService.markApiRecovered();

        return ApiResponse.ok(Map.of("healthy", true));

    }



    @PostMapping("/api-health/error")

    public ApiResponse<?> simulateApiError(@RequestParam(name = "message", defaultValue = "simulated") String message) {

        apiHealthService.markApiError(message);

        return ApiResponse.ok(Map.of("healthy", false));

    }

}


