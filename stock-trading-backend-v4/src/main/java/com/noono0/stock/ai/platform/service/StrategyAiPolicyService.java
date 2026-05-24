package com.noono0.stock.ai.platform.service;

import com.noono0.stock.strategy.domain.enums.RecommendedAction;
import com.noono0.stock.strategy.domain.enums.StrategyType;
import org.springframework.stereotype.Service;

/**
 * 전략별 AI fallback 정책 — AI 캐시 없을 때.
 */
@Service
public class StrategyAiPolicyService {

    public record AiFallbackPolicy(boolean allowRuleOnly, RecommendedAction whenNoAi, String note) {}

    public AiFallbackPolicy policyFor(String strategyType) {
        StrategyType type;
        try {
            type = StrategyType.valueOf(strategyType);
        } catch (IllegalArgumentException illegalArgumentException) {
            return new AiFallbackPolicy(true, RecommendedAction.WATCH_ONLY, "unknown-strategy");
        }
        return switch (type) {
            case OPENING_BET ->
                    new AiFallbackPolicy(
                            true,
                            RecommendedAction.WATCH_ONLY,
                            "시가베팅: 장전 AI만 사용, 없으면 WATCH_ONLY");
            case SUPPLY_SCALPING ->
                    new AiFallbackPolicy(
                            true,
                            RecommendedAction.WATCH_ONLY,
                            "수급단타: 최근 1~3일 AI만, 없으면 Rule만");
            case CLOSING_BET ->
                    new AiFallbackPolicy(
                            true,
                            RecommendedAction.WATCH_ONLY,
                            "종가베팅: 캐시 우선, Fast AI는 별도 옵션");
            case SHORT_SWING ->
                    new AiFallbackPolicy(
                            false,
                            RecommendedAction.NO_ACTION,
                            "단기스윙: 장마감 Multi AI 필요");
            default -> new AiFallbackPolicy(true, RecommendedAction.WATCH_ONLY, "default");
        };
    }

    public boolean isAiAvoidDecision(String decision) {
        if (decision == null) {
            return false;
        }
        String d = decision.toUpperCase();
        return d.contains("AVOID") || d.contains("RISK") || d.contains("SELL");
    }
}
