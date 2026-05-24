package com.noono0.stock.execution;

import com.noono0.stock.execution.runtime.ExecutionRuntimeConfig;
import com.noono0.stock.execution.runtime.ExecutionRuntimeService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderGatewayRealTradingBlockTest {

    @Test
    void blocksRealWhenRealTradingDisabled() {
        ExecutionRuntimeService runtime = mock(ExecutionRuntimeService.class);
        ExecutionRuntimeConfig cfg = new ExecutionRuntimeConfig();
        cfg.setRealTradingEnabled(false);
        when(runtime.get()).thenReturn(cfg);
        when(runtime.currentPhase()).thenReturn(ExecutionPhase.REAL_AUTO);
        OrderGateway gateway = new OrderGateway(runtime);

        assertThrows(OrderGatewayException.class, () -> gateway.assertKisOrderAllowed("real"));
    }
}
