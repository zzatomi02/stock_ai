package com.noono0.stock.ai.platform.enums;

/** AI API 호출 허용 시점 */
public enum AiExecutionTiming {
    /** 08:10~08:55 장전 기업 분석 */
    PRE_MARKET,
    /** 09:00~15:30 비동기 배치(전략 스레드와 분리) */
    INTRADAY_ASYNC,
    /** 14:30~15:10 종가 후보 */
    CLOSING_CANDIDATE,
    /** 15:40+ 장마감 심층 */
    POST_MARKET,
    OFF_HOURS,
    /** 수동(장외만 동기 호출) */
    MANUAL,
    /** 빠른 매수 판단 경로 — API 호출 금지 */
    INTRADAY_BLOCKED
}
