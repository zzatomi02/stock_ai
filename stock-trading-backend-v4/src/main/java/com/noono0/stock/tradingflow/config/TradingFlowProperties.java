package com.noono0.stock.tradingflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.trading.flow")
public class TradingFlowProperties {
    /** 시스템 관심종목·익일 후보용 사용자 ID */
    private String systemUserId = "system";

    private boolean preMarketPipelineEnabled = true;
    private boolean postMarketPipelineEnabled = true;

    /** 뉴스 수집 후 strategy_signal 자동 분석 (권장, 단일 경로) */
    private boolean ingestTriggersStrategyAnalysis = true;

    /**
     * strategy 분석이 꺼져 있을 때만 trading_signal 생성.
     * strategy 분석이 켜져 있으면 무시됩니다.
     */
    private boolean legacyTradingSignalOnIngest = false;

    /** 장전 파이프라인에서 종목별 뉴스 수집 */
    private boolean preMarketNewsCollectEnabled = true;

    private int preMarketNewsDisplayPerStock = 5;

    private String preMarketNewsQuerySuffix = " 주식";

    /** 장전 파이프라인에서 DART 공시 수집 */
    private boolean preMarketDisclosureCollectEnabled = true;

    private int preMarketDisclosureLookbackDays = 7;
}
