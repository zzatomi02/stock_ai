package com.noono0.stock.strategy.dto;

import com.noono0.stock.strategy.domain.enums.MarketCondition;
import com.noono0.stock.strategy.domain.enums.MarketTimeWindow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 현재 거래 컨텍스트 + 전략별 판단 목록 */
public record TradingContextResponse(
        String tradeDate,
        String currentTime,
        int marketMoodScore,
        MarketCondition marketCondition,
        String marketConditionLabel,
        MarketTimeWindow marketTimeWindow,
        String marketTimeWindowLabel,
        String formulaDescription,
        List<StrategyDecisionDto> strategies) {

    public static final String FORMULA =
            "최종 신호 = (전략 ON/OFF × 오늘임시 × 시장규칙 × 시간규칙) × (뉴스+수급+기술−리스크) × (시장가중치×시간가중치)";

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tradeDate", tradeDate);
        m.put("currentTime", currentTime);
        m.put("marketMoodScore", marketMoodScore);
        m.put("marketCondition", marketCondition != null ? marketCondition.name() : null);
        m.put("marketConditionLabel", marketConditionLabel);
        m.put("marketTimeWindow", marketTimeWindow != null ? marketTimeWindow.name() : null);
        m.put("marketTimeWindowLabel", marketTimeWindowLabel);
        m.put("formula", FORMULA);
        m.put("strategies", strategies.stream().map(StrategyDecisionDto::toMap).toList());
        return m;
    }
}
