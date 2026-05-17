package com.noono0.stock.ops.web;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.execution.OrderGateway;
import com.noono0.stock.integration.kis.ws.KisRealtimeFacade;
import com.noono0.stock.ops.schedule.ScheduleMonitor;
import com.noono0.stock.ops.trading.TradingControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ops")
@RequiredArgsConstructor
public class OpsController {
    private final TradingControlService tradingControlService;
    private final KisRealtimeFacade kisRealtimeFacade;
    private final ScheduleMonitor scheduleMonitor;
    private final OrderGateway orderGateway;

    @GetMapping("/execution-phase")
    public ApiResponse<?> executionPhase() {
        return ApiResponse.ok(
                java.util.Map.of(
                        "phase", orderGateway.currentPhase().name(),
                        "observeOnly", orderGateway.allowsSignalOnly()));
    }

    @GetMapping("/emergency-stop")
    public ApiResponse<?> getEmergency() {
        return ApiResponse.ok(tradingControlService.isEmergencyStop());
    }

    @PostMapping("/emergency-stop")
    public ApiResponse<?> setEmergency(@RequestParam("enabled") boolean enabled) {
        tradingControlService.setEmergencyStop(enabled);
        return ApiResponse.ok(enabled, enabled ? "긴급 중지 켜짐" : "긴급 중지 해제");
    }

    @GetMapping("/realtime-ws")
    public ApiResponse<?> realtime() {
        return ApiResponse.ok(kisRealtimeFacade.status());
    }

    @GetMapping("/schedules")
    public ApiResponse<?> schedules() {
        return ApiResponse.ok(scheduleMonitor.snapshot());
    }
}
