package com.noono0.stock.strategy.service;

import com.noono0.stock.strategy.domain.StrategyMaster;
import com.noono0.stock.strategy.domain.enums.MarketCondition;
import com.noono0.stock.strategy.domain.enums.MarketTimeWindow;
import com.noono0.stock.strategy.domain.enums.StrategyType;
import com.noono0.stock.strategy.dto.EffectiveStrategyDto;
import com.noono0.stock.strategy.dto.MarketConditionAnalysis;
import com.noono0.stock.strategy.dto.StrategySelection;
import com.noono0.stock.strategy.repository.StrategyMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyExecutionResolver {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StrategyMasterRepository masterRepository;
    private final MarketConditionAnalyzer marketConditionAnalyzer;
    private final StrategySelector strategySelector;

    public MarketConditionAnalysis currentMarketAnalysis() {
        return marketConditionAnalyzer.analyze();
    }

    public List<EffectiveStrategyDto> resolveAll(LocalDateTime at) {
        LocalDateTime kst = at.atZone(KST).toLocalDateTime();
        MarketConditionAnalysis market = marketConditionAnalyzer.analyze();
        MarketTimeWindow window = MarketTimeWindow.current(kst.toLocalTime());
        log.debug(
                "[STRATEGY-RESOLVE] at={} market={} window={}",
                kst,
                market.condition(),
                window);

        List<EffectiveStrategyDto> out = new ArrayList<>();
        for (StrategyMaster master : masterRepository.findAllByOrderBySortOrderAscIdAsc()) {
            out.add(resolveOne(master, kst.toLocalDate(), kst.toLocalTime(), market, window));
        }
        return out;
    }

    public Optional<EffectiveStrategyDto> resolveByCode(String strategyType, LocalDateTime at) {
        return masterRepository
                .findByStrategyType(strategyType)
                .map(
                        m -> {
                            LocalDateTime kst = at.atZone(KST).toLocalDateTime();
                            MarketConditionAnalysis market = marketConditionAnalyzer.analyze();
                            return resolveOne(
                                    m,
                                    kst.toLocalDate(),
                                    kst.toLocalTime(),
                                    market,
                                    MarketTimeWindow.current(kst.toLocalTime()));
                        });
    }

    public boolean isStrategyAllowedForBuy(String strategyType, LocalDateTime at) {
        return resolveByCode(strategyType, at).map(EffectiveStrategyDto::allowBuy).orElse(false);
    }

    public boolean hasAnyBuyStrategyAllowed(LocalDateTime at) {
        return resolveAll(at).stream().anyMatch(EffectiveStrategyDto::allowBuy);
    }

    private EffectiveStrategyDto resolveOne(
            StrategyMaster master,
            LocalDate tradeDate,
            LocalTime time,
            MarketConditionAnalysis market,
            MarketTimeWindow window) {
        StrategyType typeEnum = parseType(master.getStrategyType());
        boolean buyStrategy = typeEnum != null && typeEnum.buyStrategy();

        StrategySelection selection =
                strategySelector.select(master, tradeDate, market.condition(), window);

        boolean blockBuy =
                buyStrategy
                        && (window == MarketTimeWindow.OPENING_NO_TRADE
                                || !selection.timeWindowEnabled()
                                || selection.timeWeightMultiplier() <= 0.001);

        double combinedWeight = selection.combinedWeightMultiplier();
        boolean effectiveEnabled = selection.executable() && combinedWeight > 0.01;
        boolean allowBuy = effectiveEnabled && buyStrategy && !blockBuy;

        Boolean todayOverride = selection.hasRuntimeOverride() ? selection.runtimeEnabled() : null;

        String reason =
                buildReason(market, selection, window, blockBuy, effectiveEnabled, allowBuy);

        return new EffectiveStrategyDto(
                master.getStrategyType(),
                master.getStrategyName(),
                typeEnum != null ? typeEnum.name() : "TRADE",
                buyStrategy,
                selection.masterEnabled(),
                todayOverride,
                selection.todayRuntimeEnabled(),
                market.condition().name(),
                selection.marketRuleEnabled(),
                selection.marketWeightMultiplier(),
                window.name(),
                window.label(),
                selection.timeWindowEnabled(),
                selection.timeWeightMultiplier(),
                blockBuy,
                combinedWeight,
                effectiveEnabled,
                allowBuy,
                reason);
    }

    private static StrategyType parseType(String code) {
        if (code == null) {
            return null;
        }
        try {
            return StrategyType.valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String buildReason(
            MarketConditionAnalysis market,
            StrategySelection selection,
            MarketTimeWindow window,
            boolean blockBuy,
            boolean effectiveEnabled,
            boolean allowBuy) {
        StringBuilder sb = new StringBuilder();
        sb.append("시장=").append(market.conditionLabel()).append("(").append(market.marketScore()).append("점)");
        sb.append(" | ").append(selection.reasonSummary());
        sb.append(" | 가중치=시장×")
                .append(selection.marketWeightMultiplier())
                .append(" 시간×")
                .append(selection.timeWeightMultiplier());
        if (blockBuy) {
            sb.append(" | 매수차단(").append(window.label()).append(")");
        }
        sb.append(" → ").append(effectiveEnabled ? "활성" : "비활성");
        if (allowBuy) {
            sb.append(", 매수허용");
        }
        return sb.toString();
    }
}
