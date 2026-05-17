package com.noono0.stock.ai.platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai.platform")
public class AiPlatformProperties {
    private boolean batchEnabled = true;
    private int batchMaxJobsPerTick = 5;
    private int batchMaxStocksPerTick = 8;
    private String defaultOpenAiModel = "gpt-4o-mini";
    /** Rule 점수 비중 (0.80 = AI 20%) */
    private double ruleScoreWeight = 0.80;
    private double aiScoreWeight = 0.20;

    /** 종가베팅(15:10~15:20) 캐시 없을 때 동기 Fast AI */
    private boolean closingBetFastAiEnabled = true;
    private int closingBetFastAiTimeoutMs = 3000;
    private int closingBetFastAiMaxTokens = 800;
}
