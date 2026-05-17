package com.noono0.stock.execution.config;

import com.noono0.stock.execution.ExecutionPhase;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.execution")
public class ExecutionPhaseProperties {
    /** 기본 OBSERVE — 실전 주문 차단 */
    private String phase = "OBSERVE";
    /** 최종 점수 임계값 (매수 후보) */
    private int buyScoreThreshold = 62;
    /** 최종 점수 임계값 (매도 경고) */
    private int sellScoreThreshold = 38;

    public ExecutionPhase resolvedPhase() {
        return ExecutionPhase.fromConfig(phase);
    }
}
