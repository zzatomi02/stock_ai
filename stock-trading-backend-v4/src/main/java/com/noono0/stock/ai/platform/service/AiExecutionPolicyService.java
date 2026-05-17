package com.noono0.stock.ai.platform.service;

import com.noono0.stock.ai.platform.enums.AiExecutionTiming;
import com.noono0.stock.strategy.service.TradingClockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

/**
 * AI API 호출 시점 정책.
 *
 * <ul>
 *   <li>빠른 매수 판단: API 호출 금지 — DB 캐시만
 *   <li>배치 스케줄: PRE_MARKET / INTRADAY_ASYNC / CLOSING / POST_MARKET 허용
 *   <li>수동 MANUAL: 장외만
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AiExecutionPolicyService {

    private final TradingClockService tradingClock;

    public boolean isLiveApiCallAllowed(AiExecutionTiming timing) {
        if (timing == AiExecutionTiming.INTRADAY_BLOCKED) {
            return false;
        }
        if (timing == AiExecutionTiming.INTRADAY_ASYNC) {
            return isIntradayAsyncWindow();
        }
        if (timing == AiExecutionTiming.CLOSING_CANDIDATE) {
            return isClosingWindow();
        }
        if (timing == AiExecutionTiming.CLOSING_BET_FAST) {
            return isClosingBetWindow();
        }
        if (timing == AiExecutionTiming.PRE_MARKET) {
            return isPreMarketWindow();
        }
        if (timing == AiExecutionTiming.POST_MARKET) {
            return isPostMarketWindow();
        }
        if (timing == AiExecutionTiming.MANUAL) {
            return !isIntradayAsyncWindow();
        }
        return timing == AiExecutionTiming.OFF_HOURS;
    }

    public AiExecutionTiming resolveCurrentTiming() {
        if (isPreMarketWindow()) return AiExecutionTiming.PRE_MARKET;
        if (isIntradayAsyncWindow()) return AiExecutionTiming.INTRADAY_ASYNC;
        if (isClosingWindow()) return AiExecutionTiming.CLOSING_CANDIDATE;
        if (isPostMarketWindow()) return AiExecutionTiming.POST_MARKET;
        LocalTime t = tradingClock.currentTime();
        if (t.isBefore(LocalTime.of(9, 0))) return AiExecutionTiming.PRE_MARKET;
        if (t.isAfter(LocalTime.of(15, 30))) return AiExecutionTiming.POST_MARKET;
        return AiExecutionTiming.OFF_HOURS;
    }

    /** 동기 매매 판단 경로 — 장중 API 금지 */
    public void assertFastPathNoLiveApi() {
        if (isIntradayAsyncWindow()) {
            throw new IllegalStateException("장중 빠른 판단에서는 AI API를 호출할 수 없습니다. DB 캐시만 조회하세요.");
        }
    }

    public void assertLiveApiAllowed(AiExecutionTiming timing) {
        if (!isLiveApiCallAllowed(timing)) {
            throw new IllegalStateException(
                    "AI API 호출 불가 (timing=" + timing + "). 빠른 판단은 캐시 조회만 사용하세요.");
        }
    }

    public boolean isPreMarketWindow() {
        LocalTime t = tradingClock.currentTime();
        return !t.isBefore(LocalTime.of(8, 10)) && t.isBefore(LocalTime.of(8, 55));
    }

    public boolean isIntradayAsyncWindow() {
        LocalTime t = tradingClock.currentTime();
        return !t.isBefore(LocalTime.of(9, 0)) && t.isBefore(LocalTime.of(15, 30));
    }

    public boolean isClosingWindow() {
        LocalTime t = tradingClock.currentTime();
        return !t.isBefore(LocalTime.of(14, 30)) && t.isBefore(LocalTime.of(15, 10));
    }

    /** 15:10~15:20 종가베팅 */
    public boolean isClosingBetWindow() {
        LocalTime t = tradingClock.currentTime();
        return !t.isBefore(LocalTime.of(15, 10)) && t.isBefore(LocalTime.of(15, 20));
    }

    public boolean isPostMarketWindow() {
        LocalTime t = tradingClock.currentTime();
        return t.isAfter(LocalTime.of(15, 40)) || t.isBefore(LocalTime.of(6, 0));
    }
}
