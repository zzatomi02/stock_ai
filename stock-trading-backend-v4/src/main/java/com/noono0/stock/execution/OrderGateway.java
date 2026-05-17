package com.noono0.stock.execution;

import com.noono0.stock.execution.config.ExecutionPhaseProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderGateway {
    private final ExecutionPhaseProperties properties;

    public ExecutionPhase currentPhase() {
        return properties.resolvedPhase();
    }

    public void assertKisOrderAllowed(String kisMode) {
        if ("real".equalsIgnoreCase(kisMode)) {
            throw new OrderGatewayException(
                    "REAL_TRADING 정책: 실전(real) 주문은 아직 허용되지 않습니다. OBSERVE·KIS_PAPER·VIRTUAL을 사용하세요.");
        }
        ExecutionPhase p = currentPhase();
        if (p == ExecutionPhase.OBSERVE || p == ExecutionPhase.VIRTUAL) {
            throw new OrderGatewayException(
                    "현재 실행 단계(" + p + ")에서는 KIS 주문을 할 수 없습니다. 관찰/가상 모드입니다.");
        }
        if (p == ExecutionPhase.KIS_PAPER && !"paper".equalsIgnoreCase(kisMode)) {
            throw new OrderGatewayException(
                    "KIS 모의투자 단계에서는 paper 모드로만 주문할 수 있습니다.");
        }
        if ((p == ExecutionPhase.REAL_MANUAL || p == ExecutionPhase.REAL_AUTO)
                && !"real".equalsIgnoreCase(kisMode)) {
            throw new OrderGatewayException(
                    "실전 단계(" + p + ")에서는 real 모드로만 주문할 수 있습니다.");
        }
        if (p == ExecutionPhase.REAL_MANUAL) {
            throw new OrderGatewayException(
                    "실전 수동 승인 단계입니다. UI 승인 API(향후)를 통해서만 주문할 수 있습니다.");
        }
    }

    public void assertAutoTradeAllowed() {
        if (currentPhase() != ExecutionPhase.REAL_AUTO) {
            throw new OrderGatewayException(
                    "자동매매는 REAL_AUTO 단계에서만 허용됩니다. 현재: " + currentPhase());
        }
    }

    public boolean allowsSignalOnly() {
        ExecutionPhase p = currentPhase();
        return p == ExecutionPhase.OBSERVE || p == ExecutionPhase.VIRTUAL;
    }
}
