package com.noono0.stock.order.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.order.service.OrderReasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/reasons")
@RequiredArgsConstructor
public class OrderReasonController {
    private final OrderReasonService orderReasonService;

    @GetMapping("/signal/{signalId}")
    public ApiResponse<?> bySignal(@PathVariable("signalId") long signalId) {
        return ApiResponse.ok(orderReasonService.findBySignalId(signalId).orElse(null));
    }
}
