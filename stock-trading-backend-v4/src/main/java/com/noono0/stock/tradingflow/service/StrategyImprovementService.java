package com.noono0.stock.tradingflow.service;

import com.noono0.stock.strategy.domain.StrategySignal;
import com.noono0.stock.strategy.repository.StrategySignalRepository;
import com.noono0.stock.strategy.service.TradingClockService;
import com.noono0.stock.tradingflow.domain.StrategyImprovementRecommendation;
import com.noono0.stock.tradingflow.repository.StrategyImprovementRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StrategyImprovementService {

    private final TradingClockService tradingClock;
    private final StrategySignalRepository strategySignalRepository;
    private final StrategyImprovementRecommendationRepository recommendationRepository;

    @Transactional
    public int generateFromToday() {
        var today = tradingClock.today();
        List<StrategySignal> all =
                strategySignalRepository.findByTradeDateAndStatusOrderByAdjustedFinalScoreDesc(
                        today, "CANDIDATE");
        Map<String, long[]> stats = new HashMap<>();
        for (StrategySignal s : all) {
            stats.computeIfAbsent(s.getStrategyCode(), k -> new long[3]);
            long[] arr = stats.get(s.getStrategyCode());
            arr[0]++;
            if (Boolean.FALSE.equals(s.getRiskCheckPassed())) {
                arr[1]++;
            }
            if (s.getAiDecision() != null && s.getAiDecision().contains("AVOID")) {
                arr[2]++;
            }
        }
        int n = 0;
        for (var e : stats.entrySet()) {
            StrategyImprovementRecommendation rec = new StrategyImprovementRecommendation();
            rec.setTradeDate(today);
            rec.setStrategyType(e.getKey());
            long total = e.getValue()[0];
            long riskBlocked = e.getValue()[1];
            long aiAvoid = e.getValue()[2];
            rec.setEvidenceSummary(
                    "CANDIDATE="
                            + total
                            + " riskBlocked="
                            + riskBlocked
                            + " aiAvoid="
                            + aiAvoid);
            if (riskBlocked > total / 2) {
                rec.setRecommendation("리스크 차단 비율이 높습니다. minScoreThreshold 상향 또는 포지션 한도 재검토를 권장합니다.");
            } else if (aiAvoid > 0) {
                rec.setRecommendation("AI AVOID/RISK 판단이 있었습니다. 해당 전략의 AI 가중치를 낮추거나 WATCH_ONLY 정책을 강화하세요.");
            } else {
                rec.setRecommendation("당일 시그널 품질 양호. 현 설정 유지 후 백테스트로 검증하세요.");
            }
            recommendationRepository.save(rec);
            n++;
        }
        return n;
    }
}
