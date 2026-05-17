package com.noono0.stock.tradingflow.service;

import com.noono0.stock.strategy.domain.StrategySignal;
import com.noono0.stock.strategy.repository.StrategySignalRepository;
import com.noono0.stock.strategy.service.TradingClockService;
import com.noono0.stock.tradingflow.config.TradingFlowProperties;
import com.noono0.stock.watchlist.service.UserWatchlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 관심종목 준비 — 당일 CANDIDATE·고득점 시그널을 시스템 watchlist에 반영.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchlistPreparationService {

    private final TradingFlowProperties flowProperties;
    private final TradingClockService tradingClock;
    private final StrategySignalRepository strategySignalRepository;
    private final UserWatchlistService watchlistService;

    @Transactional
    public int prepareFromTodaySignals() {
        return prepareFromDate(tradingClock.today(), 70);
    }

    @Transactional
    public int prepareNextDayFromPostMarket() {
        return prepareFromDate(tradingClock.today(), 65);
    }

    @Transactional
    public int prepareFromDate(LocalDate tradeDate, int minAdjustedScore) {
        String userId = flowProperties.getSystemUserId();
        List<StrategySignal> signals =
                strategySignalRepository.findByTradeDateAndStatusOrderByAdjustedFinalScoreDesc(
                        tradeDate, "CANDIDATE");
        Set<String> added = new LinkedHashSet<>();
        int count = 0;
        for (StrategySignal s : signals) {
            if (!StringUtils.hasText(s.getStockCode())) {
                continue;
            }
            int score =
                    s.getAdjustedFinalScore() != null ? s.getAdjustedFinalScore().intValue() : 0;
            if (score < minAdjustedScore) {
                continue;
            }
            if (!added.add(s.getStockCode())) {
                continue;
            }
            try {
                watchlistService.add(
                        userId,
                        s.getStockCode(),
                        s.getStockName() != null ? s.getStockName() : s.getStockCode(),
                        count);
                count++;
            } catch (Exception e) {
                log.debug("[WATCHLIST] skip {} — {}", s.getStockCode(), e.getMessage());
            }
        }
        log.info("[WATCHLIST] user={} added/updated {} stocks from {}", userId, count, tradeDate);
        return count;
    }
}
