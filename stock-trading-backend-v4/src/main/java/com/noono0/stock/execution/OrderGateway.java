package com.noono0.stock.execution;

import com.noono0.stock.execution.runtime.ExecutionRuntimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderGateway {
    private final ExecutionRuntimeService executionRuntimeService;

    public ExecutionPhase currentPhase() {
        return executionRuntimeService.currentPhase();
    }

    public void assertKisOrderAllowed(String kisMode) {
        ExecutionPhase p = currentPhase();
        if (p.isObserveOnly()) {
            throw new OrderGatewayException(
                    "현재 실행 단계(" + p + ")에서는 KIS 주문을 할 수 없습니다. 종목 추천·분석만 합니다.");
        }
        if ("real".equalsIgnoreCase(kisMode)) {
            if (!executionRuntimeService.get().isRealTradingEnabled()) {
                throw new OrderGatewayException(
                        "실전 주문이 비활성입니다. 설정에서 real-trading-enabled 를 켜거나 단계를 모의로 변경하세요.");
            }
            if (p != ExecutionPhase.REAL_ALERT && p != ExecutionPhase.REAL_AUTO) {
                throw new OrderGatewayException("실전 주문은 REAL_ALERT 또는 REAL_AUTO 단계에서만 가능합니다.");
            }
        }
        if (p.requiresHumanApproval()) {
            throw new OrderGatewayException(
                    "현재 단계("
                            + p
                            + ")는 추천·승인 모드입니다. 종목 추천 화면에서 승인 후 주문하세요.");
        }
        String expected = p.kisMode();
        if (!expected.equalsIgnoreCase(kisMode)) {
            throw new OrderGatewayException(
                    "단계 " + p + " 에서는 " + expected + " 모드로만 주문할 수 있습니다. 요청: " + kisMode);
        }
    }

    /** 승인 API 전용 — ALERT 단계에서만 */
    public void assertApprovalOrderAllowed(String kisMode) {
        ExecutionPhase p = currentPhase();
        if (!p.requiresHumanApproval()) {
            throw new OrderGatewayException("승인 주문은 PAPER_ALERT / REAL_ALERT 단계에서만 가능합니다. 현재: " + p);
        }
        if ("real".equalsIgnoreCase(kisMode) && !executionRuntimeService.get().isRealTradingEnabled()) {
            throw new OrderGatewayException("실전 주문이 비활성입니다.");
        }
        String expected = p.kisMode();
        if (!expected.equalsIgnoreCase(kisMode)) {
            throw new OrderGatewayException("단계 " + p + " 에서는 " + expected + " 모드만 허용됩니다.");
        }
    }

    public void assertAutoTradeAllowed() {
        ExecutionPhase p = currentPhase();
        if (!p.allowsAutoOrder()) {
            throw new OrderGatewayException(
                    "자동매매는 PAPER_AUTO / REAL_AUTO 단계에서만 허용됩니다. 현재: " + p);
        }
    }

    public boolean allowsSignalOnly() {
        return currentPhase().isObserveOnly();
    }

    public boolean requiresHumanApproval() {
        return currentPhase().requiresHumanApproval();
    }
}
