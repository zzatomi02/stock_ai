package com.noono0.stock.risk.service;

import com.noono0.stock.risk.config.RiskProperties;
import com.noono0.stock.risk.domain.RiskDailyState;
import com.noono0.stock.risk.domain.enums.RiskEventType;
import com.noono0.stock.risk.repository.RiskDailyStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiskDailyStateService {

    private final RiskDailyStateRepository repository;
    private final RiskProperties properties;
    private final RiskEventService riskEventService;

    @Transactional(readOnly = true)
    public RiskDailyState getOrCreateToday(String userId) {
        String uid = userId != null ? userId : "default";
        return repository
                .findByTradeDateAndUserId(LocalDate.now(), uid)
                .orElseGet(() -> repository.save(newState(uid)));
    }

    @Transactional
    public RiskDailyState incrementBuy(String userId) {
        RiskDailyState state = getOrCreateToday(userId);
        state.setBuyOrderCount(state.getBuyOrderCount() + 1);
        return repository.save(state);
    }

    @Transactional
    public RiskDailyState incrementSell(String userId) {
        RiskDailyState state = getOrCreateToday(userId);
        state.setSellOrderCount(state.getSellOrderCount() + 1);
        return repository.save(state);
    }

    @Transactional
    public RiskDailyState recordRealizedPnl(String userId, BigDecimal delta) {
        RiskDailyState state = getOrCreateToday(userId);
        BigDecimal pnl = state.getRealizedPnl().add(delta);
        state.setRealizedPnl(pnl);
        evaluateLossLimits(state);
        return repository.save(state);
    }

    @Transactional
    public void haltBuy(String userId, String reason) {
        RiskDailyState state = getOrCreateToday(userId);
        state.setBuyHalted(true);
        state.setHaltReason(reason);
        repository.save(state);
    }

    public double dailyLossRatePercent(RiskDailyState state) {
        if (state.getReferenceEquity() == null
                || state.getReferenceEquity().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal loss = state.getRealizedPnl().negate();
        if (loss.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return loss.multiply(BigDecimal.valueOf(100))
                .divide(state.getReferenceEquity(), 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void evaluateLossLimits(RiskDailyState state) {
        double lossRate = dailyLossRatePercent(state);
        long lossAmount = state.getRealizedPnl().compareTo(BigDecimal.ZERO) < 0
                ? state.getRealizedPnl().abs().longValue()
                : 0;
        if (lossRate >= properties.getDailyMaxLossRatePercent()) {
            state.setBuyHalted(true);
            state.setHaltReason("일일 손실률 " + lossRate + "% 도달");
            riskEventService.record(
                    RiskEventType.DAILY_LOSS_LIMIT_REACHED,
                    "CRITICAL",
                    state.getHaltReason(),
                    null,
                    null,
                    Map.of("lossRatePercent", lossRate));
        } else if (lossAmount >= properties.getDailyMaxLossAmount()) {
            state.setBuyHalted(true);
            state.setHaltReason("일일 손실금액 " + lossAmount + "원 도달");
            riskEventService.record(
                    RiskEventType.DAILY_LOSS_AMOUNT_REACHED,
                    "CRITICAL",
                    state.getHaltReason(),
                    null,
                    null,
                    Map.of("lossAmount", lossAmount));
        }
    }

    private RiskDailyState newState(String userId) {
        RiskDailyState s = new RiskDailyState();
        s.setTradeDate(LocalDate.now());
        s.setUserId(userId);
        s.setReferenceEquity(BigDecimal.valueOf(properties.getReferenceEquity()));
        s.setRealizedPnl(BigDecimal.ZERO);
        return s;
    }
}
