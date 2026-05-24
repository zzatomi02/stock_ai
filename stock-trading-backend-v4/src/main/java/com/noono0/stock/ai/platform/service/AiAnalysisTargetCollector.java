package com.noono0.stock.ai.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.ai.platform.enums.AiBatchPhase;
import com.noono0.stock.market.domain.MarketTop100Snapshot;
import com.noono0.stock.market.dto.TopStockDto;
import com.noono0.stock.market.repository.MarketTop100SnapshotRepository;
import com.noono0.stock.news.domain.NewsArticle;
import com.noono0.stock.news.repository.NewsArticleJpaRepository;
import com.noono0.stock.strategy.domain.StrategySignal;
import com.noono0.stock.strategy.repository.StrategySignalRepository;
import com.noono0.stock.strategy.service.TradingClockService;
import com.noono0.stock.watchlist.domain.UserWatchlist;
import com.noono0.stock.watchlist.repository.UserWatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisTargetCollector {

    private final TradingClockService tradingClock;
    private final UserWatchlistRepository watchlistRepository;
    private final MarketTop100SnapshotRepository top100Repository;
    private final NewsArticleJpaRepository newsRepository;
    private final StrategySignalRepository strategySignalRepository;
    private final CachedAiAnalysisQueryService cachedAiQuery;
    private final ObjectMapper objectMapper;

    public record StockTarget(String stockCode, String stockName, String reason) {}

    public List<StockTarget> collect(AiBatchPhase phase) {
        Set<String> seen = new LinkedHashSet<>();
        Map<String, StockTarget> map = new LinkedHashMap<>();

        switch (phase) {
            case PRE_MARKET -> {
                addAll(map, seen, watchlistTargets(), "관심종목");
                addAll(map, seen, top100Targets("amount"), "전일 거래대금 상위");
                addAll(map, seen, top100Targets("volume"), "전일 거래량 상위");
                addAll(map, seen, recentNewsTargets(3), "최근 뉴스 급증");
            }
            case INTRADAY_ASYNC -> {
                addAll(map, seen, recentNewsTargets(1), "신규 뉴스");
                addAll(map, seen, signalsWithoutAi(), "AI 미분석 신호");
            }
            case CLOSING_CANDIDATE -> {
                addAll(map, seen, top100Targets("amount"), "막판 거래대금");
                addAll(map, seen, recentNewsTargets(2), "최근 뉴스/공시");
                addAll(map, seen, watchlistTargets(), "종가 후보 관심");
            }
            case POST_MARKET -> {
                addAll(map, seen, todayTradedSignals(), "당일 전략 신호");
                addAll(map, seen, watchlistTargets(), "내일 관심 후보");
            }
        }
        log.info("[AI-TARGET] phase={} count={}", phase, map.size());
        return List.copyOf(map.values());
    }

    private List<StockTarget> watchlistTargets() {
        return watchlistRepository.findAll().stream()
                .filter(w -> StringUtils.hasText(w.getStockCode()))
                .map(w -> new StockTarget(w.getStockCode(), w.getStockName(), "watchlist"))
                .distinct()
                .toList();
    }

    private List<StockTarget> top100Targets(String type) {
        return top100Repository
                .findByType(type)
                .map(this::parseTop100)
                .orElse(List.of())
                .stream()
                .map(t -> new StockTarget(t.stockCode(), t.stockName(), "top100-" + type))
                .toList();
    }

    private List<TopStockDto> parseTop100(MarketTop100Snapshot snap) {
        if (!StringUtils.hasText(snap.getPayloadJson())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(snap.getPayloadJson(), new TypeReference<List<TopStockDto>>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<StockTarget> recentNewsTargets(int days) {
        LocalDateTime since = tradingClock.now().minusDays(days);
        return newsRepository.findByCollectedAtAfterOrderByCollectedAtDesc(since).stream()
                .filter(a -> StringUtils.hasText(a.getStockCode()))
                .map(a -> new StockTarget(a.getStockCode(), name(a), "news"))
                .distinct()
                .limit(30)
                .toList();
    }

    private List<StockTarget> signalsWithoutAi() {
        LocalDate today = tradingClock.today();
        return strategySignalRepository.findByTradeDateAndStatusOrderByAdjustedFinalScoreDesc(today, "CANDIDATE")
                .stream()
                .filter(s -> StringUtils.hasText(s.getStockCode()))
                .filter(s -> cachedAiQuery.getBestForTrading(s.getStockCode()).isEmpty())
                .map(s -> new StockTarget(s.getStockCode(), s.getStockName(), "signal-no-ai"))
                .limit(20)
                .toList();
    }

    private List<StockTarget> todayTradedSignals() {
        LocalDate today = tradingClock.today();
        return strategySignalRepository
                .findByTradeDateAndStatusOrderByAdjustedFinalScoreDesc(today, "CANDIDATE")
                .stream()
                .filter(s -> StringUtils.hasText(s.getStockCode()))
                .map(s -> new StockTarget(s.getStockCode(), s.getStockName(), "today-signal"))
                .distinct()
                .toList();
    }

    private static String name(NewsArticle a) {
        return StringUtils.hasText(a.getLlmStockName()) ? a.getLlmStockName() : a.getTitle();
    }

    private static void addAll(
            Map<String, StockTarget> map, Set<String> seen, List<StockTarget> list, String reasonPrefix) {
        for (StockTarget t : list) {
            if (seen.add(t.stockCode())) {
                map.put(t.stockCode(), new StockTarget(t.stockCode(), t.stockName(), reasonPrefix + ":" + t.reason()));
            }
        }
    }
}
