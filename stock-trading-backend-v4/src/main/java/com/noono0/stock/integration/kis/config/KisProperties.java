package com.noono0.stock.integration.kis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter @Setter
@ConfigurationProperties(prefix = "app.kis")
public class KisProperties {
    private String mode;
    private String accountNo;
    private String productCode;
    private Credential paper = new Credential();
    private Credential real = new Credential();
    /** WebSocket 실시간 시세 (승인키 + ops 호스트) */
    private Realtime realtime = new Realtime();

    @Getter @Setter
    public static class Credential {
        private String appKey;
        private String appSecret;
    }

    @Getter @Setter
    public static class Realtime {
        /** 켜면 부팅 후 백그라운드에서 WS 연결·구독 시도 */
        private boolean enabled;
        /** H0STCNT0 tr_key (6자리 종목코드) */
        private String subscribeStock = "005930";
        /** 비우면 모드에 따라 ops 21000(실전) / 31000(모의) */
        private String wsUrl = "";
    }

    public Credential selected() {
        return "real".equalsIgnoreCase(mode) ? real : paper;
    }

    /** {@link com.noono0.stock.common.web.TradingModeFilter} 로 요청별 모드가 있으면 그에 맞는 키를 쓴다. */
    public Credential forTradingModeOverride(String tradingModeAttr) {
        if (!StringUtils.hasText(tradingModeAttr)) {
            return selected();
        }
        return "real".equalsIgnoreCase(tradingModeAttr.trim()) ? real : paper;
    }
}
