package com.noono0.stock.strategy.service;

import com.noono0.stock.strategy.domain.*;
import com.noono0.stock.strategy.dto.*;
import com.noono0.stock.strategy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StrategySettingsService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StrategyMasterRepository masterRepository;
    private final StrategyRuntimeSettingRepository runtimeRepository;
    private final MarketConditionStrategyRepository marketConditionRepository;
    private final StrategyTimeWindowRuleRepository timeWindowRepository;
    private final StrategyDecisionEngine strategyDecisionEngine;

    @Transactional(readOnly = true)
    public List<StrategySettingDto> listStrategySettings() {
        return masterRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(StrategySettingDto::from)
                .toList();
    }

    @Transactional
    public StrategySettingDto updateMasterEnabled(String strategyType, boolean isEnabled) {
        StrategyMaster master = requireMaster(strategyType);
        master.setEnabled(isEnabled);
        return StrategySettingDto.from(masterRepository.save(master));
    }

    @Transactional(readOnly = true)
    public List<StrategyRuntimeSettingDto> listRuntimeSettings(LocalDate tradeDate) {
        if (tradeDate == null) {
            tradeDate = LocalDate.now(KST);
        }
        LocalDate date = tradeDate;
        return runtimeRepository.findAll().stream()
                .filter(r -> date.equals(r.getTradeDate()))
                .map(StrategyRuntimeSettingDto::from)
                .sorted(Comparator.comparing(StrategyRuntimeSettingDto::strategyType))
                .toList();
    }

    @Transactional
    public StrategyRuntimeSetting saveRuntimeSetting(StrategyRuntimeSettingRequest req) {
        requireMaster(req.strategyType());
        StrategyRuntimeSetting entity =
                runtimeRepository
                        .findByTradeDateAndStrategyType(req.tradeDate(), req.strategyType())
                        .orElseGet(
                                () -> {
                                    StrategyRuntimeSetting n = new StrategyRuntimeSetting();
                                    n.setTradeDate(req.tradeDate());
                                    n.setStrategyType(req.strategyType());
                                    return n;
                                });
        entity.setEnabled(req.isEnabled());
        entity.setReason(req.reason());
        entity.setUpdatedBy(req.updatedBy());
        return runtimeRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<MarketConditionStrategyDto> listMarketConditionStrategies(
            String marketCondition, String strategyType) {
        List<MarketConditionStrategy> rows;
        if (StringUtils.hasText(marketCondition) && StringUtils.hasText(strategyType)) {
            rows =
                    marketConditionRepository.findByMarketConditionAndStrategyTypeOrderByIdAsc(
                            marketCondition, strategyType);
        } else if (StringUtils.hasText(marketCondition)) {
            rows = marketConditionRepository.findByMarketCondition(marketCondition);
        } else if (StringUtils.hasText(strategyType)) {
            rows = marketConditionRepository.findByStrategyType(strategyType);
        } else {
            rows = marketConditionRepository.findAll();
        }
        return rows.stream().sorted(Comparator.comparing(MarketConditionStrategy::getId)).map(MarketConditionStrategyDto::from).toList();
    }

    @Transactional
    public MarketConditionStrategyDto updateMarketConditionStrategy(
            long id, MarketConditionStrategyUpdateRequest req) {
        MarketConditionStrategy entity =
                marketConditionRepository
                        .findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("시장별 전략 설정을 찾을 수 없습니다: " + id));
        entity.setEnabled(req.isEnabled());
        entity.setWeightMultiplier(scaleWeight(req.weightMultiplier()));
        entity.setMinScoreOverride(req.minScoreOverride());
        entity.setDescription(req.description());
        return MarketConditionStrategyDto.from(marketConditionRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<StrategyTimeWindowDto> listStrategyTimeWindows(String strategyType, String marketTimeWindow) {
        List<StrategyTimeWindowRule> rows;
        if (StringUtils.hasText(strategyType) && StringUtils.hasText(marketTimeWindow)) {
            rows =
                    timeWindowRepository.findByStrategyTypeAndMarketTimeWindowOrderByIdAsc(
                            strategyType, marketTimeWindow);
        } else if (StringUtils.hasText(strategyType)) {
            rows = timeWindowRepository.findByStrategyType(strategyType);
        } else if (StringUtils.hasText(marketTimeWindow)) {
            rows = timeWindowRepository.findByMarketTimeWindow(marketTimeWindow);
        } else {
            rows = timeWindowRepository.findAll();
        }
        return rows.stream().sorted(Comparator.comparing(StrategyTimeWindowRule::getId)).map(StrategyTimeWindowDto::from).toList();
    }

    @Transactional
    public StrategyTimeWindowDto updateStrategyTimeWindow(long id, StrategyTimeWindowUpdateRequest req) {
        StrategyTimeWindowRule entity =
                timeWindowRepository
                        .findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("시간대별 전략 설정을 찾을 수 없습니다: " + id));
        entity.setEnabled(req.isEnabled());
        entity.setWeightMultiplier(scaleWeight(req.weightMultiplier()));
        entity.setMinScoreOverride(req.minScoreOverride());
        entity.setMaxTradeCount(req.maxTradeCount());
        entity.setDescription(req.description());
        return StrategyTimeWindowDto.from(timeWindowRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public EnabledNowResponse getEnabledNow() {
        TradingContextResponse ctx = strategyDecisionEngine.evaluateContext();
        return strategyDecisionEngine.toEnabledNowResponse(ctx);
    }

    @Transactional(readOnly = true)
    public TradingContextResponse getDecisionContext() {
        return strategyDecisionEngine.evaluateContext();
    }

    private StrategyMaster requireMaster(String strategyType) {
        return masterRepository
                .findByStrategyType(strategyType)
                .orElseThrow(() -> new IllegalArgumentException("전략을 찾을 수 없습니다: " + strategyType));
    }

    private static BigDecimal scaleWeight(BigDecimal weight) {
        if (weight == null) {
            return BigDecimal.ONE;
        }
        double v = weight.doubleValue();
        if (v < 0) {
            v = 0;
        }
        if (v > 2) {
            v = 2;
        }
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
