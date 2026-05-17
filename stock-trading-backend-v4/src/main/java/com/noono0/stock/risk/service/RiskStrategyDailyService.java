package com.noono0.stock.risk.service;

import com.noono0.stock.risk.config.RiskProperties;
import com.noono0.stock.risk.domain.RiskStrategyDailyState;
import com.noono0.stock.risk.domain.enums.RiskEventType;
import com.noono0.stock.risk.repository.RiskStrategyDailyStateRepository;
import com.noono0.stock.strategy.domain.StrategyRuntimeSetting;
import com.noono0.stock.strategy.repository.StrategyRuntimeSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiskStrategyDailyService {

    private final RiskStrategyDailyStateRepository repository;
    private final StrategyRuntimeSettingRepository runtimeRepository;
    private final RiskProperties properties;
    private final RiskEventService riskEventService;

    @Transactional(readOnly = true)
    public RiskStrategyDailyState getOrCreate(String strategyType) {
        return repository
                .findByTradeDateAndStrategyType(LocalDate.now(), strategyType)
                .orElseGet(
                        () -> {
                            RiskStrategyDailyState s = new RiskStrategyDailyState();
                            s.setTradeDate(LocalDate.now());
                            s.setStrategyType(strategyType);
                            return repository.save(s);
                        });
    }

    @Transactional(readOnly = true)
    public boolean isDisabledToday(String strategyType) {
        return repository
                .findByTradeDateAndStrategyType(LocalDate.now(), strategyType)
                .map(RiskStrategyDailyState::isDisabledToday)
                .orElse(false);
    }

    @Transactional
    public void recordTrade(String strategyType) {
        RiskStrategyDailyState s = getOrCreate(strategyType);
        s.setTradeCount(s.getTradeCount() + 1);
        repository.save(s);
    }

    /** 연속 손절 1건 기록 — 한도 도달 시 오늘 전략 OFF */
    @Transactional
    public void recordStopLoss(String strategyType) {
        RiskStrategyDailyState s = getOrCreate(strategyType);
        s.setConsecutiveStopLoss(s.getConsecutiveStopLoss() + 1);
        if (s.getConsecutiveStopLoss() >= properties.getConsecutiveStopLossLimit()) {
            disableStrategyToday(s, "연속 손절 " + s.getConsecutiveStopLoss() + "회");
        }
        repository.save(s);
    }

    @Transactional
    public void resetStopLossStreak(String strategyType) {
        repository.findByTradeDateAndStrategyType(LocalDate.now(), strategyType).ifPresent(s -> {
            s.setConsecutiveStopLoss(0);
            repository.save(s);
        });
    }

    private void disableStrategyToday(RiskStrategyDailyState s, String reason) {
        s.setDisabledToday(true);
        s.setDisabledReason(reason);
        StrategyRuntimeSetting runtime =
                runtimeRepository
                        .findByTradeDateAndStrategyType(LocalDate.now(), s.getStrategyType())
                        .orElseGet(
                                () -> {
                                    StrategyRuntimeSetting n = new StrategyRuntimeSetting();
                                    n.setTradeDate(LocalDate.now());
                                    n.setStrategyType(s.getStrategyType());
                                    return n;
                                });
        runtime.setEnabled(false);
        runtime.setReason(reason);
        runtime.setUpdatedBy("RISK_ENGINE");
        runtimeRepository.save(runtime);
        riskEventService.record(
                RiskEventType.STRATEGY_AUTO_DISABLED,
                "HIGH",
                reason,
                null,
                s.getStrategyType(),
                Map.of("consecutiveStopLoss", s.getConsecutiveStopLoss()));
    }
}
