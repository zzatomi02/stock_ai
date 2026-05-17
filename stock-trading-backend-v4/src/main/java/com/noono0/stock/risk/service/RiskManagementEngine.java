package com.noono0.stock.risk.service;

import com.noono0.stock.broker.mapper.BrokerOrderAttemptMapper;
import com.noono0.stock.integration.kis.service.KisApiHealthService;
import com.noono0.stock.market.service.MarketMoodService;
import com.noono0.stock.risk.config.RiskProperties;
import com.noono0.stock.risk.domain.RiskDailyState;
import com.noono0.stock.risk.domain.enums.RiskEventType;
import com.noono0.stock.risk.dto.RiskCheckResult;
import com.noono0.stock.risk.dto.WarningFilterResult;
import com.noono0.stock.strategy.service.TradingClockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 리스크 관리 엔진 — 주문 전 필수 검사.
 *
 * <ul>
 *   <li>일일 손실률·손실금액
 *   <li>종목/전략 포지션 한도
 *   <li>일·시간대 매매 횟수
 *   <li>재진입 쿨다운
 *   <li>연속 손절 → 전략 OFF
 *   <li>시장 급락·API/시세 장애
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskManagementEngine {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RiskProperties properties;
    private final RiskDailyStateService dailyStateService;
    private final RiskStrategyDailyService strategyDailyService;
    private final RiskEventService riskEventService;
    private final MarketWarningFilter marketWarningFilter;
    private final MarketMoodService marketMoodService;
    private final KisApiHealthService apiHealthService;
    private final BrokerOrderAttemptMapper orderAttemptMapper;
    private final TradingClockService tradingClock;

    public record OrderRiskContext(
            String userId,
            String stockCode,
            String side,
            String strategyType,
            long orderAmount,
            int quantity) {}

    public RiskCheckResult validateOrder(OrderRiskContext ctx) {
        RiskCheckResult result = new RiskCheckResult();
        String side = ctx.side() != null ? ctx.side().toUpperCase() : "";
        boolean isBuy = "BUY".equals(side);

        checkApiHealth(result);
        checkQuoteDelay(result);

        RiskDailyState daily = dailyStateService.getOrCreateToday(ctx.userId());
        result.pass("dailyState", Map.of("buyHalted", daily.isBuyHalted(), "buyCount", daily.getBuyOrderCount()));

        if (daily.isGlobalHalt()) {
            result.fail("globalHalt", "GLOBAL_HALT", "전역 주문 중지: " + daily.getHaltReason(), daily.getHaltReason());
            return result;
        }

        if (isBuy && daily.isBuyHalted()) {
            result.fail(
                    "dailyLossLimit",
                    "DAILY_LOSS_HALT",
                    "일일 손실 한도 도달 — 신규 매수 중지. " + daily.getHaltReason(),
                    daily.getHaltReason());
            return result;
        }

        if (ctx.orderAmount() > properties.getSingleOrderMaxAmount()) {
            result.fail(
                    "singleOrderLimit",
                    "SINGLE_ORDER_LIMIT",
                    "1회 주문 한도 초과 (" + properties.getSingleOrderMaxAmount() + "원)",
                    ctx.orderAmount());
        } else {
            result.pass("singleOrderLimit", ctx.orderAmount());
        }

        if (isBuy) {
            checkDailyBuyCount(ctx, result, daily);
            checkTimeWindowTradeLimit(result);
            checkMarketCrash(result);
            checkSymbolPosition(ctx, result);
            checkStrategyPosition(ctx, result);
            checkReentryCooldown(ctx, result);
            checkStrategyDisabled(ctx, result);
            checkMarketWarning(ctx, result);
        }

        if (!result.passed()) {
            riskEventService.record(
                    RiskEventType.ORDER_VALIDATION_FAILED,
                    "MEDIUM",
                    result.failReason(),
                    ctx.stockCode(),
                    ctx.strategyType(),
                    result.checks());
        }
        return result;
    }

    private void checkApiHealth(RiskCheckResult result) {
        if (properties.isHaltOnApiError() && !apiHealthService.isApiHealthy()) {
            result.fail(
                    "apiHealth",
                    "API_ERROR",
                    "KIS/API 장애 — 주문 중지. " + apiHealthService.getLastErrorMessage(),
                    apiHealthService.getLastErrorMessage());
            riskEventService.record(
                    RiskEventType.API_ERROR_ORDER_HALT,
                    "CRITICAL",
                    "API 장애로 주문 차단",
                    null,
                    null,
                    Map.of("message", apiHealthService.getLastErrorMessage()));
        } else {
            result.pass("apiHealth", apiHealthService.isApiHealthy());
        }
    }

    private void checkQuoteDelay(RiskCheckResult result) {
        long delay = apiHealthService.quoteDelayMs();
        if (delay > properties.getQuoteDelayMaxMs()) {
            result.fail(
                    "quoteDelay",
                    "QUOTE_DELAY",
                    "시세 지연 " + delay + "ms — 주문 차단",
                    delay);
            riskEventService.record(
                    RiskEventType.QUOTE_DELAY_HALT,
                    "HIGH",
                    "시세 지연으로 주문 차단",
                    null,
                    null,
                    Map.of("delayMs", delay));
        } else {
            result.pass("quoteDelay", delay);
        }
    }

    private void checkDailyBuyCount(OrderRiskContext ctx, RiskCheckResult result, RiskDailyState daily) {
        int count = daily.getBuyOrderCount();
        int max = properties.getDailyMaxBuyCount();
        if (count >= max) {
            result.fail(
                    "dailyBuyCount",
                    "DAILY_TRADE_LIMIT",
                    "일일 매수 횟수 한도 (" + max + "회)",
                    count);
            riskEventService.record(
                    RiskEventType.DAILY_TRADE_LIMIT_REACHED,
                    "HIGH",
                    "일일 매수 횟수 한도",
                    ctx.stockCode(),
                    ctx.strategyType(),
                    Map.of("count", count, "max", max));
        } else {
            result.pass("dailyBuyCount", Map.of("current", count, "max", max));
        }
    }

    private void checkTimeWindowTradeLimit(RiskCheckResult result) {
        LocalTime now = tradingClock.currentTime();
        for (RiskProperties.TimeWindowTradeLimit w : properties.getTimeWindowLimits()) {
            LocalTime start = LocalTime.parse(w.getStart());
            LocalTime end = LocalTime.parse(w.getEnd());
            if (!now.isBefore(start) && now.isBefore(end)) {
                LocalDate today = tradingClock.today();
                LocalDateTime from = today.atTime(start);
                LocalDateTime to = today.atTime(end);
                int count = orderAttemptMapper.countBySideAndCreatedAtBetween("BUY", from, to);
                if (count >= w.getMaxTrades()) {
                    result.fail(
                            "timeWindowTradeLimit",
                            "TIME_WINDOW_LIMIT",
                            w.getStart() + "~" + w.getEnd() + " 구간 매수 " + w.getMaxTrades() + "회 한도",
                            Map.of("window", w.getStart() + "-" + w.getEnd(), "count", count));
                    riskEventService.record(
                            RiskEventType.TIME_WINDOW_TRADE_LIMIT,
                            "MEDIUM",
                            "시간대별 매매 횟수 한도",
                            null,
                            null,
                            Map.of("count", count, "max", w.getMaxTrades()));
                } else {
                    result.pass(
                            "timeWindowTradeLimit",
                            Map.of("window", w.getStart() + "-" + w.getEnd(), "count", count, "max", w.getMaxTrades()));
                }
            }
        }
    }

    private void checkMarketCrash(RiskCheckResult result) {
        int mood = marketMoodService.currentScore();
        if (mood <= properties.getMarketCrashMoodThreshold()) {
            result.fail(
                    "marketCrash",
                    "MARKET_CRASH",
                    "시장 급락(분위기 " + mood + ") — 신규 매수 중지",
                    mood);
            riskEventService.record(
                    RiskEventType.MARKET_CRASH_BUY_HALT,
                    "HIGH",
                    "시장 급락 매수 중지",
                    null,
                    null,
                    Map.of("moodScore", mood));
        } else {
            result.pass("marketCrash", mood);
        }
    }

    private void checkSymbolPosition(OrderRiskContext ctx, RiskCheckResult result) {
        BigDecimal max = properties.maxPositionPerSymbolAmount();
        if (ctx.orderAmount() > max.longValue()) {
            result.fail(
                    "symbolPositionLimit",
                    "SYMBOL_POSITION",
                    "종목당 최대 " + properties.getMaxPositionPerSymbolPercent() + "% 초과",
                    Map.of("orderAmount", ctx.orderAmount(), "max", max));
            riskEventService.record(
                    RiskEventType.SYMBOL_POSITION_LIMIT,
                    "MEDIUM",
                    "종목 포지션 한도",
                    ctx.stockCode(),
                    null,
                    Map.of("orderAmount", ctx.orderAmount()));
        } else {
            result.pass("symbolPositionLimit", Map.of("max", max));
        }
    }

    private void checkStrategyPosition(OrderRiskContext ctx, RiskCheckResult result) {
        if (!StringUtils.hasText(ctx.strategyType())) {
            result.pass("strategyPositionLimit", "N/A");
            return;
        }
        long max = properties.getMaxPositionPerStrategyAmount();
        if (ctx.orderAmount() > max) {
            result.fail(
                    "strategyPositionLimit",
                    "STRATEGY_POSITION",
                    "전략당 최대 매수금액 " + max + "원 초과",
                    ctx.orderAmount());
        } else {
            result.pass("strategyPositionLimit", max);
        }
    }

    private void checkReentryCooldown(OrderRiskContext ctx, RiskCheckResult result) {
        LocalDateTime after =
                tradingClock.now().minusMinutes(properties.getReentryCooldownMinutes());
        int recentSells =
                orderAttemptMapper.countByStockCodeAndSideAndCreatedAtAfter(
                        ctx.stockCode(), "SELL", after);
        if (recentSells > 0) {
            result.fail(
                    "reentryCooldown",
                    "REENTRY_COOLDOWN",
                    "동일 종목 재진입 쿨다운 "
                            + properties.getReentryCooldownMinutes()
                            + "분 (최근 매도 있음)",
                    ctx.stockCode());
            riskEventService.record(
                    RiskEventType.REENTRY_COOLDOWN,
                    "MEDIUM",
                    "재진입 쿨다운",
                    ctx.stockCode(),
                    ctx.strategyType(),
                    Map.of("recentSells", recentSells));
        } else {
            result.pass("reentryCooldown", "ok");
        }
    }

    private void checkStrategyDisabled(OrderRiskContext ctx, RiskCheckResult result) {
        if (!StringUtils.hasText(ctx.strategyType())) {
            return;
        }
        if (strategyDailyService.isDisabledToday(ctx.strategyType())) {
            result.fail(
                    "strategyDisabled",
                    "STRATEGY_OFF",
                    "전략 오늘 비활성(연속 손절 등): " + ctx.strategyType(),
                    ctx.strategyType());
        } else {
            result.pass("strategyDisabled", false);
        }
    }

    private void checkMarketWarning(OrderRiskContext ctx, RiskCheckResult result) {
        WarningFilterResult w = marketWarningFilter.evaluate(ctx.stockCode());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("buyAllowed", w.buyAllowed());
        detail.put("warningType", w.warningType());
        detail.put("riskScoreAdjustment", w.riskScoreAdjustment());
        detail.put("riskReason", w.riskReason());
        result.pass("marketWarning", detail);
        if (!w.buyAllowed()) {
            result.fail(
                    "marketWarningBuyBlock",
                    "WARNING_BLOCK",
                    "시장경보 매수 금지: " + w.riskReason(),
                    w.warningType());
        }
    }

    /** 주문 성공 후 호출 */
    public void onOrderAccepted(OrderRiskContext ctx) {
        if ("BUY".equalsIgnoreCase(ctx.side())) {
            dailyStateService.incrementBuy(ctx.userId());
            if (StringUtils.hasText(ctx.strategyType())) {
                strategyDailyService.recordTrade(ctx.strategyType());
            }
        } else if ("SELL".equalsIgnoreCase(ctx.side())) {
            dailyStateService.incrementSell(ctx.userId());
        }
    }
}
