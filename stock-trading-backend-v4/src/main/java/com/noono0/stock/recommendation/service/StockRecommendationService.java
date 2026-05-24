package com.noono0.stock.recommendation.service;

import com.noono0.stock.broker.service.KisOrderRecordService;
import com.noono0.stock.execution.ExecutionPhase;
import com.noono0.stock.execution.OrderGateway;
import com.noono0.stock.execution.OrderGatewayException;
import com.noono0.stock.execution.runtime.ExecutionRuntimeConfig;
import com.noono0.stock.execution.runtime.ExecutionRuntimeService;
import com.noono0.stock.ops.trading.TradingControlService;
import com.noono0.stock.recommendation.domain.StockRecommendation;
import com.noono0.stock.recommendation.dto.StockRecommendationDto;
import com.noono0.stock.recommendation.repository.StockRecommendationRepository;
import com.noono0.stock.strategy.domain.StrategySignal;
import com.noono0.stock.strategy.repository.StrategySignalRepository;
import com.noono0.stock.strategy.service.TradingClockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockRecommendationService {

    private static final Set<String> SIGNAL_GRADES = Set.of("S", "A", "B", "C", "D");

    private final StockRecommendationRepository stockRecommendationRepository;
    private final StrategySignalRepository strategySignalRepository;
    private final ExecutionRuntimeService executionRuntimeService;
    private final OrderGateway orderGateway;
    private final TradingControlService tradingControlService;
    private final TradingClockService tradingClockService;
    private final RecommendationNotificationService recommendationNotificationService;
    private final KisOrderRecordService kisOrderRecordService;

    public List<StockRecommendationDto> listToday(String statusFilter) {
        LocalDate tradeDate = tradingClockService.today();
        List<StockRecommendation> recommendations =
                StringUtils.hasText(statusFilter)
                        ? stockRecommendationRepository.findByTradeDateAndStatusOrderByBestAdjustedScoreDesc(
                                tradeDate, statusFilter.trim().toUpperCase())
                        : stockRecommendationRepository.findByTradeDateOrderByBestAdjustedScoreDesc(tradeDate);
        return recommendations.stream().map(StockRecommendationDto::from).toList();
    }

    public StockRecommendationDto get(long recommendationId) {
        StockRecommendation recommendation =
                stockRecommendationRepository
                        .findById(recommendationId)
                        .orElseThrow(() -> new IllegalArgumentException("추천 없음: " + recommendationId));
        return StockRecommendationDto.from(recommendation);
    }

    /**
     * 오늘 CANDIDATE BUY 시그널 → 종목 단위 추천 목록 갱신. 신규·갱신 건 알림(설정·단계에 따름).
     */
    @Transactional
    public SyncResult syncFromStrategySignals() {
        if (tradingControlService.isEmergencyStop()) {
            return new SyncResult(0, 0, 0, "긴급정지 ON");
        }
        ExecutionRuntimeConfig runtimeConfig = executionRuntimeService.get();
        ExecutionRuntimeConfig.RecommendationRules recommendationRules = runtimeConfig.getRecommendation();
        LocalDate tradeDate = tradingClockService.today();

        if (stockRecommendationRepository.countByTradeDate(tradeDate) >= recommendationRules.getMaxPerDay()) {
            return new SyncResult(0, 0, 0, "일일 추천 상한 도달");
        }

        List<StrategySignal> buyCandidateSignals =
                strategySignalRepository
                        .findByTradeDateAndStatusOrderByAdjustedFinalScoreDesc(tradeDate, "CANDIDATE")
                        .stream()
                        .filter(signal -> "BUY".equalsIgnoreCase(signal.getSide()))
                        .filter(signal -> StringUtils.hasText(signal.getStockCode()))
                        .filter(signal -> passesRecommendationRules(signal, recommendationRules))
                        .toList();

        Map<String, List<StrategySignal>> signalsGroupedByStock =
                buyCandidateSignals.stream()
                        .collect(
                                Collectors.groupingBy(
                                        signal -> signal.getStockCode().trim(),
                                        LinkedHashMap::new,
                                        Collectors.toList()));

        int createdCount = 0;
        int updatedCount = 0;
        int notifiedCount = 0;
        ExecutionPhase executionPhase = executionRuntimeService.currentPhase();

        for (Map.Entry<String, List<StrategySignal>> stockEntry : signalsGroupedByStock.entrySet()) {
            if (stockRecommendationRepository.countByTradeDate(tradeDate) >= recommendationRules.getMaxPerDay()) {
                break;
            }
            String stockCode = stockEntry.getKey();
            List<StrategySignal> signalsForStock = stockEntry.getValue();
            StrategySignal topSignal = signalsForStock.get(0);
            Optional<StockRecommendation> existingRecommendation =
                    stockRecommendationRepository.findByTradeDateAndStockCode(tradeDate, stockCode);

            if (existingRecommendation.isPresent() && recommendationRules.isDedupeByStock()) {
                StockRecommendation recommendation = existingRecommendation.get();
                if (mergeIfBetterScore(recommendation, topSignal, signalsForStock)) {
                    updatedCount++;
                }
                continue;
            }

            StockRecommendation newRecommendation =
                    buildNewRecommendation(tradeDate, topSignal, signalsForStock, executionPhase);
            stockRecommendationRepository.save(newRecommendation);
            createdCount++;

            if (shouldSendNotification(executionPhase, newRecommendation)) {
                recommendationNotificationService.notifyNewRecommendation(newRecommendation);
                newRecommendation.setNotificationSent(true);
                newRecommendation.setNotifiedAt(tradingClockService.now());
                stockRecommendationRepository.save(newRecommendation);
                notifiedCount++;
            }
        }

        log.info(
                "【REC】 sync created={} updated={} notified={}",
                createdCount,
                updatedCount,
                notifiedCount);
        return new SyncResult(createdCount, updatedCount, notifiedCount, "ok");
    }

    @Transactional
    public StockRecommendationDto approve(long recommendationId, String userId) {
        StockRecommendation recommendation =
                stockRecommendationRepository
                        .findById(recommendationId)
                        .orElseThrow(() -> new IllegalArgumentException("추천 없음: " + recommendationId));
        if (!"PENDING_APPROVAL".equals(recommendation.getStatus())) {
            throw new IllegalStateException("승인 가능 상태가 아님: " + recommendation.getStatus());
        }
        if (tradingControlService.isEmergencyStop()) {
            throw new OrderGatewayException("긴급정지 ON");
        }
        ExecutionPhase executionPhase = executionRuntimeService.currentPhase();
        String kisTradingMode = executionPhase.kisMode();
        orderGateway.assertApprovalOrderAllowed(kisTradingMode);

        int orderQuantity = Math.max(1, executionRuntimeService.get().getOrderQty());
        String orderReasoning = "종목추천 승인 id=" + recommendationId + " by=" + userId;
        long linkedSignalId =
                recommendation.getPrimaryStrategySignalId() != null
                        ? recommendation.getPrimaryStrategySignalId()
                        : recommendationId;
        try {
            var orderResult =
                    kisOrderRecordService.placeBuyForApprovedRecommendation(
                            linkedSignalId,
                            recommendation.getStockCode(),
                            orderQuantity,
                            orderReasoning,
                            extractFirstStrategyCode(recommendation.getStrategyCodes()));
            recommendation.setStatus("ORDERED");
            recommendation.setApprovedAt(tradingClockService.now());
            recommendation.setApprovedBy(userId);
            recommendation.setBrokerOrderAttemptId(orderResult.attemptId());
            stockRecommendationRepository.save(recommendation);
            return StockRecommendationDto.from(recommendation);
        } catch (Exception exception) {
            recommendation.setStatus("ORDER_FAILED");
            stockRecommendationRepository.save(recommendation);
            throw exception;
        }
    }

    @Transactional
    public StockRecommendationDto reject(long recommendationId, String userId, String rejectReason) {
        StockRecommendation recommendation =
                stockRecommendationRepository
                        .findById(recommendationId)
                        .orElseThrow(() -> new IllegalArgumentException("추천 없음: " + recommendationId));
        if (!"PENDING_APPROVAL".equals(recommendation.getStatus())
                && !"RECOMMENDED".equals(recommendation.getStatus())) {
            throw new IllegalStateException("거절 가능 상태가 아님: " + recommendation.getStatus());
        }
        recommendation.setStatus("REJECTED");
        recommendation.setRejectedAt(tradingClockService.now());
        recommendation.setRejectedBy(userId);
        recommendation.setRejectReason(StringUtils.hasText(rejectReason) ? rejectReason.trim() : null);
        stockRecommendationRepository.save(recommendation);
        return StockRecommendationDto.from(recommendation);
    }

    private boolean shouldSendNotification(ExecutionPhase executionPhase, StockRecommendation recommendation) {
        return executionPhase.requiresHumanApproval()
                && "PENDING_APPROVAL".equals(recommendation.getStatus())
                && !Boolean.TRUE.equals(recommendation.getNotificationSent());
    }

    private static boolean passesRecommendationRules(
            StrategySignal signal, ExecutionRuntimeConfig.RecommendationRules rules) {
        if (signal.getRiskCheckPassed() != null && !signal.getRiskCheckPassed()) {
            return false;
        }
        int adjustedScore =
                signal.getAdjustedFinalScore() != null ? signal.getAdjustedFinalScore().intValue() : 0;
        if (adjustedScore < rules.getMinAdjustedScore()) {
            return false;
        }
        String signalGrade = signal.getSignalGrade();
        if (!StringUtils.hasText(signalGrade)) {
            return true;
        }
        String normalizedGrade = signalGrade.trim().toUpperCase(Locale.ROOT);
        String minimumGrade = rules.getMinGrade().trim().toUpperCase(Locale.ROOT);
        if (!SIGNAL_GRADES.contains(normalizedGrade) || !SIGNAL_GRADES.contains(minimumGrade)) {
            return !"D".equals(normalizedGrade);
        }
        return gradeRank(normalizedGrade) <= gradeRank(minimumGrade);
    }

    private static int gradeRank(String grade) {
        return switch (grade) {
            case "S" -> 0;
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            default -> 4;
        };
    }

    private boolean mergeIfBetterScore(
            StockRecommendation recommendation,
            StrategySignal topSignal,
            List<StrategySignal> signalsForStock) {
        BigDecimal newScore = topSignal.getAdjustedFinalScore();
        if (newScore == null) {
            return false;
        }
        if (recommendation.getBestAdjustedScore() != null
                && recommendation.getBestAdjustedScore().compareTo(newScore) >= 0) {
            return false;
        }
        applySignalScores(recommendation, topSignal, signalsForStock);
        stockRecommendationRepository.save(recommendation);
        return true;
    }

    private StockRecommendation buildNewRecommendation(
            LocalDate tradeDate,
            StrategySignal topSignal,
            List<StrategySignal> signalsForStock,
            ExecutionPhase executionPhase) {
        StockRecommendation recommendation = new StockRecommendation();
        recommendation.setTradeDate(tradeDate);
        recommendation.setStockCode(topSignal.getStockCode().trim());
        recommendation.setStockName(topSignal.getStockName());
        recommendation.setStatus(initialRecommendationStatus(executionPhase));
        applySignalScores(recommendation, topSignal, signalsForStock);
        return recommendation;
    }

    private static String initialRecommendationStatus(ExecutionPhase executionPhase) {
        if (executionPhase.isObserveOnly()) {
            return "RECOMMENDED";
        }
        if (executionPhase.requiresHumanApproval()) {
            return "PENDING_APPROVAL";
        }
        return "RECOMMENDED";
    }

    private static void applySignalScores(
            StockRecommendation recommendation,
            StrategySignal topSignal,
            List<StrategySignal> signalsForStock) {
        recommendation.setBestAdjustedScore(topSignal.getAdjustedFinalScore());
        recommendation.setSignalGrade(topSignal.getSignalGrade());
        recommendation.setPrimaryStrategySignalId(topSignal.getId());
        recommendation.setStrategyCodes(
                signalsForStock.stream()
                        .map(StrategySignal::getStrategyCode)
                        .distinct()
                        .collect(Collectors.joining(",")));
        recommendation.setReasonSummary(
                StringUtils.hasText(topSignal.getStrategySelectedReason())
                        ? topSignal.getStrategySelectedReason()
                        : topSignal.getReasonMessage());
    }

    private static String extractFirstStrategyCode(String strategyCodesCsv) {
        if (!StringUtils.hasText(strategyCodesCsv)) {
            return null;
        }
        int commaIndex = strategyCodesCsv.indexOf(',');
        return commaIndex > 0 ? strategyCodesCsv.substring(0, commaIndex) : strategyCodesCsv;
    }

    public record SyncResult(int created, int updated, int notified, String message) {}
}
