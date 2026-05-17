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

    /** 뉴스 수집 후 strategy_signal 자동 분석 */
    private boolean ingestTriggersStrategyAnalysis = true;

    /** 장전 파이프라인에서 종목별 뉴스 수집 */
    private boolean preMarketNewsCollectEnabled = true;

    private int preMarketNewsDisplayPerStock = 5;

    private String preMarketNewsQuerySuffix = " 주식";
}
