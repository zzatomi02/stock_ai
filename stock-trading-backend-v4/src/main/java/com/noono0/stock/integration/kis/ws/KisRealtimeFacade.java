package com.noono0.stock.integration.kis.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 실시간 시세 WebSocket 상태 조회. 실제 연결은 {@link KisRealtimeWebSocketService}.
 */
@Service
@RequiredArgsConstructor
public class KisRealtimeFacade {

    private final KisRealtimeWebSocketService realtimeWebSocketService;

    public Map<String, Object> status() {
        return realtimeWebSocketService.status();
    }
}
