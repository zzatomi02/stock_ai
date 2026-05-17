package com.noono0.stock.strategy.service;

import com.noono0.stock.market.service.MarketMoodService;
import com.noono0.stock.strategy.domain.enums.MarketCondition;
import com.noono0.stock.strategy.dto.MarketConditionAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 시장 상태 판단.
 *
 * <p>우선순위: 구조적 신호(테마·대형주·개별주·변동성) → 시장점수 구간 매핑.
 * 구조적 신호는 현재 스텁이며, {@code app.market.analyzer.stub-*} 로 로컬 더미 테스트 가능.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketConditionAnalyzer {

    private final MarketMoodService marketMoodService;

    @Value("${app.market.analyzer.stub-theme:false}")
    private boolean stubTheme;

    @Value("${app.market.analyzer.stub-large-cap:false}")
    private boolean stubLargeCap;

    @Value("${app.market.analyzer.stub-small-cap:false}")
    private boolean stubSmallCap;

    @Value("${app.market.analyzer.stub-high-volatility:false}")
    private boolean stubHighVolatility;

    public MarketConditionAnalysis analyze() {
        return analyze(marketMoodService.currentScore());
    }

    /** 지정 시장 상태로 분석 (테스트·시뮬레이션용) */
    public MarketConditionAnalysis analyzeForCondition(MarketCondition condition, int marketScore) {
        return MarketConditionAnalysis.ofScore(
                condition,
                marketScore,
                "시뮬레이션 시장상태=" + condition.label());
    }

    public MarketConditionAnalysis analyze(int marketScore) {
        boolean theme = stubTheme || detectThemeConcentration(marketScore);
        boolean largeCap = stubLargeCap || detectLargeCapConcentration(marketScore);
        boolean smallCap = stubSmallCap || detectSmallCapConcentration(marketScore);
        boolean highVol = stubHighVolatility || detectHighVolatility(marketScore);

        if (highVol) {
            return structural(
                    marketScore,
                    MarketCondition.HIGH_VOLATILITY,
                    "변동성이 높은 장으로 판단",
                    theme,
                    largeCap,
                    smallCap,
                    true);
        }
        if (theme) {
            return structural(
                    marketScore,
                    MarketCondition.THEME,
                    "특정 테마 거래대금 집중",
                    true,
                    largeCap,
                    smallCap,
                    highVol);
        }
        if (largeCap) {
            return structural(
                    marketScore,
                    MarketCondition.LARGE_CAP,
                    "대형주 거래대금 집중",
                    theme,
                    true,
                    smallCap,
                    highVol);
        }
        if (smallCap) {
            return structural(
                    marketScore,
                    MarketCondition.SMALL_CAP,
                    "개별주·급등주 거래대금 집중",
                    theme,
                    largeCap,
                    true,
                    highVol);
        }

        MarketCondition byScore = fromMarketScore(marketScore);
        String reason =
                switch (byScore) {
                    case STRONG_BULL -> "시장점수 " + marketScore + " (80 이상) → 강한 상승장";
                    case BULL -> "시장점수 " + marketScore + " (60 이상) → 상승장";
                    case SIDEWAYS -> "시장점수 " + marketScore + " (40~59) → 횡보장";
                    case WEAK -> "시장점수 " + marketScore + " (25~39) → 약한 장";
                    case BEAR -> "시장점수 " + marketScore + " (25 미만) → 하락장";
                    default -> "시장점수 " + marketScore;
                };
        return MarketConditionAnalysis.ofScore(byScore, marketScore, reason);
    }

    /** 시장점수 구간 매핑 (요구사항 기준) */
    public static MarketCondition fromMarketScore(int score) {
        if (score >= 80) {
            return MarketCondition.STRONG_BULL;
        }
        if (score >= 60) {
            return MarketCondition.BULL;
        }
        if (score >= 40) {
            return MarketCondition.SIDEWAYS;
        }
        if (score >= 25) {
            return MarketCondition.WEAK;
        }
        return MarketCondition.BEAR;
    }

    private static MarketConditionAnalysis structural(
            int score,
            MarketCondition condition,
            String reason,
            boolean theme,
            boolean largeCap,
            boolean smallCap,
            boolean highVol) {
        return new MarketConditionAnalysis(
                score,
                condition,
                condition.label(),
                "MARKET_STRUCTURE",
                reason,
                theme,
                largeCap,
                smallCap,
                highVol);
    }

    /** @return KIS·섹터 거래대금 API 연동 전 false */
    protected boolean detectThemeConcentration(int marketScore) {
        return false;
    }

    /** @return KIS·시가총액별 거래대금 API 연동 전 false */
    protected boolean detectLargeCapConcentration(int marketScore) {
        return false;
    }

    /** @return 급등 종목 비율 API 연동 전 false */
    protected boolean detectSmallCapConcentration(int marketScore) {
        return false;
    }

    /** @return VIX·지수 변동성 API 연동 전, 점수만으로는 미사용 */
    protected boolean detectHighVolatility(int marketScore) {
        return false;
    }
}
