package com.noono0.stock.strategy.service;

import com.noono0.stock.risk.domain.RiskDailyState;
import com.noono0.stock.risk.service.MarketWarningFilter;
import com.noono0.stock.risk.service.RiskDailyStateService;
import com.noono0.stock.risk.service.RiskStrategyDailyService;
import com.noono0.stock.tradingflow.config.TradingFlowProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 시그널 후보 저장 전 리스크 게이트 (주문 금액 없이 경고·일손실·전략 OFF 검사).
 */
@Service
@RequiredArgsConstructor
public class StrategySignalRiskGateService {

    private final TradingFlowProperties flowProperties;
    private final MarketWarningFilter marketWarningFilter;
    private final RiskDailyStateService dailyStateService;
    private final RiskStrategyDailyService strategyDailyService;

    public record SignalRiskResult(boolean passed, String blockReason) {
        public static SignalRiskResult ok() {
            return new SignalRiskResult(true, null);
        }

        public static SignalRiskResult block(String reason) {
            return new SignalRiskResult(false, reason);
        }
    }

    public SignalRiskResult check(String stockCode, String strategyType, String side) {
        if (!"BUY".equalsIgnoreCase(side)) {
            return SignalRiskResult.ok();
        }
        String userId = flowProperties.getSystemUserId();

        var warning = marketWarningFilter.evaluate(stockCode);
        if (!warning.buyAllowed()) {
            return SignalRiskResult.block("경고종목: " + warning.riskReason());
        }

        RiskDailyState daily = dailyStateService.getOrCreateToday(userId);
        if (daily.isBuyHalted() || daily.isGlobalHalt()) {
            return SignalRiskResult.block(
                    daily.getHaltReason() != null ? daily.getHaltReason() : "일일 매수 중지");
        }

        if (strategyType != null && strategyDailyService.isDisabledToday(strategyType)) {
            return SignalRiskResult.block("전략 일일 비활성: " + strategyType);
        }

        return SignalRiskResult.ok();
    }
}
