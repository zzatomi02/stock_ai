package com.noono0.stock.strategy.service;

import com.noono0.stock.strategy.domain.enums.MarketCondition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketConditionAnalyzerTest {

    @Test
    void fromMarketScore_usesDocumentedThresholds() {
        assertEquals(MarketCondition.STRONG_BULL, MarketConditionAnalyzer.fromMarketScore(80));
        assertEquals(MarketCondition.STRONG_BULL, MarketConditionAnalyzer.fromMarketScore(100));
        assertEquals(MarketCondition.BULL, MarketConditionAnalyzer.fromMarketScore(60));
        assertEquals(MarketCondition.BULL, MarketConditionAnalyzer.fromMarketScore(79));
        assertEquals(MarketCondition.SIDEWAYS, MarketConditionAnalyzer.fromMarketScore(40));
        assertEquals(MarketCondition.SIDEWAYS, MarketConditionAnalyzer.fromMarketScore(59));
        assertEquals(MarketCondition.WEAK, MarketConditionAnalyzer.fromMarketScore(25));
        assertEquals(MarketCondition.WEAK, MarketConditionAnalyzer.fromMarketScore(39));
        assertEquals(MarketCondition.BEAR, MarketConditionAnalyzer.fromMarketScore(24));
        assertEquals(MarketCondition.BEAR, MarketConditionAnalyzer.fromMarketScore(0));
    }
}
