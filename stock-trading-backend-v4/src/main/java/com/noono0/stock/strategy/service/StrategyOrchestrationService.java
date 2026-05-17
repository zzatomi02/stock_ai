package com.noono0.stock.strategy.service;

import com.noono0.stock.strategy.domain.*;
import com.noono0.stock.strategy.dto.EffectiveStrategyDto;
import com.noono0.stock.strategy.domain.StrategyMaster;
import com.noono0.stock.strategy.domain.StrategyRuntimeSetting;
import com.noono0.stock.strategy.domain.enums.MarketCondition;
import com.noono0.stock.strategy.domain.enums.MarketTimeWindow;
import com.noono0.stock.strategy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StrategyOrchestrationService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StrategyExecutionResolver executionResolver;
    private final MarketConditionAnalyzer marketConditionAnalyzer;
    private final StrategyMasterRepository masterRepository;
    private final StrategyRuntimeSettingRepository runtimeRepository;
    private final MarketConditionStrategyRepository marketConditionRepository;
    private final StrategyTimeWindowRuleRepository timeWindowRepository;

    public Map<String, Object> snapshotNow() {
        LocalDateTime now = LocalDateTime.now(KST);
        List<EffectiveStrategyDto> strategies = executionResolver.resolveAll(now);
        var marketAnalysis = marketConditionAnalyzer.analyze();
        MarketTimeWindow window = MarketTimeWindow.current(now.toLocalTime());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("evaluatedAt", now.toString());
        m.put("tradeDate", now.toLocalDate().toString());
        m.put("marketMoodScore", marketAnalysis.marketScore());
        m.put("marketCondition", marketAnalysis.condition().name());
        m.put("marketConditionLabel", marketAnalysis.conditionLabel());
        m.put("marketConditionSource", marketAnalysis.detectionSource());
        m.put("marketConditionReason", marketAnalysis.reasonSummary());
        m.put("marketRegime", marketAnalysis.condition().name());
        m.put("marketRegimeLabel", marketAnalysis.conditionLabel());
        Map<String, String> windowMap = Map.of("code", window.name(), "label", window.label());
        m.put("currentTimeWindow", windowMap);
        m.put("currentTimeSlot", windowMap);
        m.put("strategies", strategies.stream().map(EffectiveStrategyDto::toMap).toList());
        m.put(
                "activeBuyStrategies",
                strategies.stream().filter(EffectiveStrategyDto::allowBuy).map(EffectiveStrategyDto::code).toList());
        return m;
    }

    @Transactional
    public void setBaseEnabled(String strategyType, boolean enabled) {
        StrategyMaster m =
                masterRepository
                        .findByStrategyType(strategyType)
                        .orElseThrow(() -> new IllegalArgumentException("전략을 찾을 수 없습니다: " + strategyType));
        m.setEnabled(enabled);
        masterRepository.save(m);
    }

    @Transactional
    public void setTodayOverride(String strategyType, LocalDate tradeDate, Boolean enabled, String reason) {
        StrategyRuntimeSetting o =
                runtimeRepository
                        .findByTradeDateAndStrategyType(tradeDate, strategyType)
                        .orElseGet(
                                () -> {
                                    StrategyRuntimeSetting n = new StrategyRuntimeSetting();
                                    n.setStrategyType(strategyType);
                                    n.setTradeDate(tradeDate);
                                    return n;
                                });
        o.setEnabled(enabled);
        o.setReason(reason);
        runtimeRepository.save(o);
    }

    @Transactional
    public void clearTodayOverride(String strategyType, LocalDate tradeDate) {
        runtimeRepository
                .findByTradeDateAndStrategyType(tradeDate, strategyType)
                .ifPresent(runtimeRepository::delete);
    }

    @Transactional
    public MarketConditionStrategy upsertMarketRule(
            String strategyType, MarketCondition condition, boolean enabled, double weight) {
        MarketConditionStrategy r =
                marketConditionRepository
                        .findByMarketConditionAndStrategyType(condition.name(), strategyType)
                        .orElseGet(
                                () -> {
                                    MarketConditionStrategy n = new MarketConditionStrategy();
                                    n.setStrategyType(strategyType);
                                    n.setMarketCondition(condition.name());
                                    return n;
                                });
        r.setEnabled(enabled);
        r.setWeightMultiplier(java.math.BigDecimal.valueOf(clampWeight(weight)));
        return marketConditionRepository.save(r);
    }

    @Transactional
    public StrategyTimeWindowRule upsertTimeRule(
            String strategyType,
            MarketTimeWindow window,
            boolean enabled,
            double weight) {
        StrategyTimeWindowRule r =
                timeWindowRepository
                        .findByStrategyTypeAndMarketTimeWindow(strategyType, window.name())
                        .orElseGet(
                                () -> {
                                    StrategyTimeWindowRule n = new StrategyTimeWindowRule();
                                    n.setStrategyType(strategyType);
                                    n.setMarketTimeWindow(window.name());
                                    n.setWindowName(window.label());
                                    n.setStartTime(window.start());
                                    n.setEndTime(window.end());
                                    return n;
                                });
        r.setEnabled(enabled);
        r.setWeightMultiplier(java.math.BigDecimal.valueOf(clampWeight(weight)));
        return timeWindowRepository.save(r);
    }

    public List<MarketConditionStrategy> listMarketRules(String strategyType) {
        return marketConditionRepository.findAll().stream()
                .filter(r -> strategyType.equals(r.getStrategyType()))
                .toList();
    }

    public List<StrategyTimeWindowRule> listTimeRules(String strategyType) {
        return timeWindowRepository.findAll().stream()
                .filter(r -> strategyType.equals(r.getStrategyType()))
                .toList();
    }

    private static double clampWeight(double w) {
        if (w < 0) return 0;
        if (w > 2) return 2;
        return Math.round(w * 100.0) / 100.0;
    }
}
