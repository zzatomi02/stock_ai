package com.noono0.stock.recommendation.service;

import com.noono0.stock.broker.mapper.BrokerOrderAttemptMapper;
import com.noono0.stock.broker.service.KisOrderRecordService;
import com.noono0.stock.execution.OrderGateway;
import com.noono0.stock.execution.runtime.ExecutionRuntimeService;
import com.noono0.stock.integration.kis.config.KisProperties;
import com.noono0.stock.ops.trading.TradingControlService;
import com.noono0.stock.recommendation.domain.StockRecommendation;
import com.noono0.stock.recommendation.repository.StockRecommendationRepository;
import com.noono0.stock.strategy.service.TradingClockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.List;

/** PAPER_AUTO / REAL_AUTO — RECOMMENDED 종목 추천 → 자동 모의·실전 매수 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockRecommendationAutoExecutor {

    private final ExecutionRuntimeService executionRuntimeService;
    private final StockRecommendationRepository stockRecommendationRepository;
    private final BrokerOrderAttemptMapper brokerOrderAttemptMapper;
    private final KisOrderRecordService kisOrderRecordService;
    private final OrderGateway orderGateway;
    private final TradingControlService tradingControlService;
    private final KisProperties kisProperties;
    private final TradingClockService tradingClockService;

    @Transactional
    public int scanAndOrder() {
        if (!executionRuntimeService.get().isAutoOrderScanEnabled()) {
            return 0;
        }
        if (tradingControlService.isEmergencyStop()) {
            return 0;
        }
        try {
            orderGateway.assertAutoTradeAllowed();
        } catch (Exception exception) {
            log.debug("【REC-AUTO】 {}", exception.getMessage());
            return 0;
        }
        if (!StringUtils.hasText(kisProperties.getAccountNo())) {
            return 0;
        }
        if (executionRuntimeService.get().isMarketHoursOnly() && !isWithinMarketHours()) {
            return 0;
        }

        List<StockRecommendation> recommendedStocks =
                stockRecommendationRepository.findByTradeDateAndStatusOrderByBestAdjustedScoreDesc(
                        tradingClockService.today(), "RECOMMENDED");

        int executedCount = 0;
        int maxOrdersPerScan = 1;
        for (StockRecommendation recommendation : recommendedStocks) {
            if (executedCount >= maxOrdersPerScan) {
                break;
            }
            long linkedSignalId =
                    recommendation.getPrimaryStrategySignalId() != null
                            ? recommendation.getPrimaryStrategySignalId()
                            : recommendation.getId();
            if (brokerOrderAttemptMapper.countBySignalId(linkedSignalId) > 0) {
                continue;
            }
            int orderQuantity = Math.max(1, executionRuntimeService.get().getOrderQty());
            try {
                var orderResult =
                        kisOrderRecordService.placeBuyForStrategySignalAuto(
                                linkedSignalId,
                                recommendation.getStockCode(),
                                orderQuantity,
                                "종목추천 자동매매 recId=" + recommendation.getId(),
                                extractFirstStrategyCode(recommendation.getStrategyCodes()));
                recommendation.setStatus("ORDERED");
                recommendation.setBrokerOrderAttemptId(orderResult.attemptId());
                stockRecommendationRepository.save(recommendation);
                executedCount++;
                log.warn(
                        "【REC-AUTO】 ★ 자동 매수 ★ {} {}",
                        recommendation.getStockCode(),
                        recommendation.getStockName());
            } catch (Exception exception) {
                recommendation.setStatus("ORDER_FAILED");
                stockRecommendationRepository.save(recommendation);
                log.warn(
                        "【REC-AUTO】 실패 recId={} — {}",
                        recommendation.getId(),
                        exception.getMessage());
            }
        }
        return executedCount;
    }

    private boolean isWithinMarketHours() {
        LocalTime currentTime = tradingClockService.currentTime();
        return !currentTime.isBefore(LocalTime.of(9, 0)) && currentTime.isBefore(LocalTime.of(15, 20));
    }

    private static String extractFirstStrategyCode(String strategyCodesCsv) {
        if (!StringUtils.hasText(strategyCodesCsv)) {
            return null;
        }
        int commaIndex = strategyCodesCsv.indexOf(',');
        return commaIndex > 0 ? strategyCodesCsv.substring(0, commaIndex) : strategyCodesCsv;
    }
}
