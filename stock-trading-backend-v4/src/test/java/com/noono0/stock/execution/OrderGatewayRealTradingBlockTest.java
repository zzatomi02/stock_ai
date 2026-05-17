package com.noono0.stock.execution;

import com.noono0.stock.execution.config.ExecutionPhaseProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderGatewayRealTradingBlockTest {

    @Test
    void blocksRealKisOrders() {
        ExecutionPhaseProperties props = mock(ExecutionPhaseProperties.class);
        when(props.resolvedPhase()).thenReturn(ExecutionPhase.REAL_AUTO);
        OrderGateway gateway = new OrderGateway(props);

        assertThrows(
                OrderGatewayException.class,
                () -> gateway.assertKisOrderAllowed("real"),
                "REAL_TRADING 정책으로 real 주문 차단");
    }
}
