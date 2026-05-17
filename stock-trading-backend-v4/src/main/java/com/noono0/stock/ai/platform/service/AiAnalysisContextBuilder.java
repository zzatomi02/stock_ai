package com.noono0.stock.ai.platform.service;

import com.noono0.stock.market.service.MarketMoodService;
import com.noono0.stock.news.domain.NewsArticle;
import com.noono0.stock.news.repository.NewsArticleJpaRepository;
import com.noono0.stock.strategy.service.MarketConditionAnalyzer;
import com.noono0.stock.strategy.service.TradingClockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiAnalysisContextBuilder {

    private final TradingClockService tradingClock;
    private final MarketConditionAnalyzer marketConditionAnalyzer;
    private final MarketMoodService marketMoodService;
    private final NewsArticleJpaRepository newsRepository;

    public Map<String, String> buildCompanyContext(String stockCode, String stockName) {
        Map<String, String> vars = new HashMap<>();
        var market = marketConditionAnalyzer.analyze();
        vars.put("stockCode", stockCode != null ? stockCode : "");
        vars.put("stockName", stockName != null ? stockName : stockCode);
        vars.put("sectorName", "");
        vars.put("tradeDate", tradingClock.today().toString());
        vars.put("marketCondition", market.conditionLabel() + " (score=" + market.marketScore() + ")");
        vars.put("priceSummary", "시장 분위기 점수 " + marketMoodService.currentScore());
        vars.put("supplySummary", "수급 데이터 연동 예정");
        vars.put("technicalSummary", "차트 데이터 연동 예정");
        vars.put("riskSummary", "시장경보·리스크 필터 DB 참조");

        LocalDateTime since = tradingClock.now().minusDays(3);
        List<NewsArticle> news =
                newsRepository.findByCollectedAtAfterOrderByCollectedAtDesc(since).stream()
                        .filter(
                                a ->
                                        stockCode != null
                                                && (stockCode.equals(a.getStockCode())
                                                        || (a.getTitle() != null
                                                                && a.getTitle().contains(stockCode))))
                        .limit(8)
                        .toList();
        vars.put(
                "recentNews",
                news.isEmpty()
                        ? "최근 관련 뉴스 없음"
                        : news.stream()
                                .map(n -> "- " + n.getTitle() + " / " + n.getLlmSentiment())
                                .collect(Collectors.joining("\n")));
        vars.put("recentDisclosures", "공시 연동 예정");
        return vars;
    }
}
