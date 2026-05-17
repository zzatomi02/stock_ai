package com.noono0.stock.ai.platform.service;

import com.noono0.stock.ai.platform.enums.AiAnalysisType;
import com.noono0.stock.ai.platform.enums.AiBatchPhase;
import com.noono0.stock.ai.platform.enums.AiExecutionTiming;
import com.noono0.stock.strategy.service.TradingClockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class AiValidityService {

    private final TradingClockService tradingClock;

    public record ValidityWindow(LocalDateTime validFrom, LocalDateTime validUntil) {}

    public ValidityWindow windowFor(AiExecutionTiming timing, AiAnalysisType analysisType) {
        LocalDate today = tradingClock.today();
        LocalDateTime now = tradingClock.now();
        return switch (timing) {
            case PRE_MARKET -> new ValidityWindow(now, today.atTime(15, 30));
            case INTRADAY_ASYNC -> new ValidityWindow(now, now.plusHours(6));
            case CLOSING_CANDIDATE -> new ValidityWindow(now, today.atTime(15, 30));
            case POST_MARKET -> new ValidityWindow(now, today.plusDays(7).atTime(23, 59));
            case MANUAL, OFF_HOURS -> windowForType(analysisType, now);
            default -> new ValidityWindow(now, now.plusHours(1));
        };
    }

    public ValidityWindow windowForPhase(AiBatchPhase phase) {
        return windowFor(mapPhase(phase), AiAnalysisType.COMPANY_ANALYSIS);
    }

    private static AiExecutionTiming mapPhase(AiBatchPhase phase) {
        return switch (phase) {
            case PRE_MARKET -> AiExecutionTiming.PRE_MARKET;
            case INTRADAY_ASYNC -> AiExecutionTiming.INTRADAY_ASYNC;
            case CLOSING_CANDIDATE -> AiExecutionTiming.CLOSING_CANDIDATE;
            case POST_MARKET -> AiExecutionTiming.POST_MARKET;
        };
    }

    private ValidityWindow windowForType(AiAnalysisType type, LocalDateTime from) {
        return switch (type) {
            case NEWS_ANALYSIS, DISCLOSURE_ANALYSIS -> new ValidityWindow(from, from.plusDays(3));
            case COMPANY_ANALYSIS -> new ValidityWindow(from, from.toLocalDate().atTime(15, 30));
            default -> new ValidityWindow(from, from.plusDays(5));
        };
    }

    public boolean isInBatchWindow(AiBatchPhase phase) {
        LocalTime t = tradingClock.currentTime();
        return switch (phase) {
            case PRE_MARKET -> !t.isBefore(LocalTime.of(8, 10)) && t.isBefore(LocalTime.of(8, 55));
            case INTRADAY_ASYNC -> !t.isBefore(LocalTime.of(9, 0)) && t.isBefore(LocalTime.of(15, 30));
            case CLOSING_CANDIDATE -> !t.isBefore(LocalTime.of(14, 30)) && t.isBefore(LocalTime.of(15, 10));
            case POST_MARKET -> t.isAfter(LocalTime.of(15, 40)) || t.isBefore(LocalTime.of(6, 0));
        };
    }
}
