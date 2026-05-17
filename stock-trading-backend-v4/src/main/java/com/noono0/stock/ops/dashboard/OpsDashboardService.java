package com.noono0.stock.ops.dashboard;

import com.noono0.stock.integration.kis.service.KisApiHealthService;
import com.noono0.stock.market.service.MarketMoodService;
import com.noono0.stock.risk.domain.RiskEvent;
import com.noono0.stock.risk.repository.RiskEventRepository;
import com.noono0.stock.risk.service.RiskDailyStateService;
import com.noono0.stock.strategy.dto.EnabledNowResponse;
import com.noono0.stock.strategy.service.StrategySettingsService;
import com.noono0.stock.strategy.repository.StrategySignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 운영 대시보드 집계 */
@Service
@RequiredArgsConstructor
public class OpsDashboardService {

    private final StrategySettingsService strategySettingsService;
    private final MarketMoodService marketMoodService;
    private final StrategySignalRepository strategySignalRepository;
    private final RiskDailyStateService riskDailyStateService;
    private final RiskEventRepository riskEventRepository;
    private final KisApiHealthService apiHealthService;

    public Map<String, Object> snapshot(String userId) {
        EnabledNowResponse enabled = strategySettingsService.getEnabledNow();
        LocalDate today = LocalDate.now();
        long buyCandidates =
                strategySignalRepository.countByTradeDateAndStatus(today, "CANDIDATE");
        var daily = riskDailyStateService.getOrCreateToday(userId);
        List<RiskEvent> events = riskEventRepository.findTop50ByOrderByCreatedAtDesc();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tradeDate", today.toString());
        m.put("marketCondition", enabled.marketCondition());
        m.put("marketTimeWindow", enabled.marketTimeWindow());
        m.put("marketMoodScore", marketMoodService.currentScore());
        m.put("enabledStrategies", enabled.strategies().stream().filter(s -> s.enabled()).count());
        m.put("totalStrategies", enabled.strategies().size());
        m.put("buyCandidateSignalsToday", buyCandidates);
        m.put("riskBuyHalted", daily.isBuyHalted());
        m.put("riskRealizedPnl", daily.getRealizedPnl());
        m.put("riskBuyCountToday", daily.getBuyOrderCount());
        m.put("apiHealthy", apiHealthService.isApiHealthy());
        m.put("quoteDelayMs", apiHealthService.quoteDelayMs());
        m.put("recentRiskEvents", events.stream().limit(20).toList());
        m.put("enabledStrategyDetails", enabled);
        return m;
    }
}
