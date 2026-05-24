package com.noono0.stock.execution.runtime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/** UI·API에서 변경 가능한 실행·추천 설정 (DB에 저장). */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionRuntimeConfig {

    public static final String KEY = "execution_runtime";

    /** {@link com.noono0.stock.execution.ExecutionPhase} 이름 */
    private String phase = "OBSERVE";

    private int orderQty = 1;

    /** PAPER_AUTO / REAL_AUTO 시 자동 주문 스캔 */
    private boolean autoOrderScanEnabled = false;

    private long autoOrderScanIntervalMs = 120_000L;

    private boolean marketHoursOnly = true;

    /** 실전 주문 허용 (false면 REAL_* 단계에서도 real API 차단) */
    private boolean realTradingEnabled = false;

    private RecommendationRules recommendation = new RecommendationRules();

    private NotificationChannels notification = new NotificationChannels();

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecommendationRules {
        /** 허용 최저 등급 S/A/B/C */
        private String minGrade = "C";
        private int minAdjustedScore = 62;
        private int maxPerDay = 30;
        /** 종목당 하루 1건 추천 */
        private boolean dedupeByStock = true;
        /** PAPER_ALERT / REAL_ALERT 에서 신규 추천 시 알림 */
        private boolean notifyOnNew = true;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NotificationChannels {
        private boolean telegram = true;
        private boolean discord = true;
        private boolean email = true;
        /** 카카오 — webhook URL 설정 시 HTTP POST (플레이스홀더) */
        private boolean kakao = false;
        private String kakaoWebhookUrl = "";
        /** SMS — webhook URL 설정 시 HTTP POST (플레이스홀더) */
        private boolean sms = false;
        private String smsWebhookUrl = "";
        /** 추천 알림 수신 이메일 (비우면 spring.mail.username) */
        private String emailTo = "";
    }
}
