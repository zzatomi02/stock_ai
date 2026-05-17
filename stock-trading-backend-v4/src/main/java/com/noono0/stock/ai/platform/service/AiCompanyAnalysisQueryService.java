package com.noono0.stock.ai.platform.service;

import com.noono0.stock.ai.platform.domain.AiCompanyAnalysis;
import com.noono0.stock.ai.platform.dto.CachedAiCompanyView;
import com.noono0.stock.ai.platform.repository.AiCompanyAnalysisQueryRepository;
import com.noono0.stock.ai.platform.repository.AiMultiConsensusResultRepository;
import com.noono0.stock.ai.platform.enums.AiAnalysisType;
import com.noono0.stock.strategy.service.TradingClockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiCompanyAnalysisQueryService {

    private final AiCompanyAnalysisQueryRepository analysisRepository;
    private final AiMultiConsensusResultRepository consensusRepository;
    private final CachedAiAnalysisQueryService cachedAiQuery;
    private final TradingClockService tradingClock;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listToday(String stockCode, String decision, String providerType, Double minScore) {
        LocalDate today = tradingClock.today();
        BigDecimal min = minScore != null ? BigDecimal.valueOf(minScore) : null;
        return analysisRepository
                .findTodayFiltered(
                        today,
                        StringUtils.hasText(stockCode) ? stockCode : null,
                        StringUtils.hasText(decision) ? decision : null,
                        StringUtils.hasText(providerType) ? providerType : null,
                        min)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(String stockCode, LocalDate tradeDate) {
        LocalDate d = tradeDate != null ? tradeDate : tradingClock.today();
        Map<String, Object> m = cachedAiQuery.getTradingBundle(stockCode, d);
        m.put(
                "providerAnalyses",
                analysisRepository.findTodayFiltered(d, stockCode, null, null, null).stream()
                        .map(this::toDetail)
                        .collect(Collectors.toList()));
        return m;
    }

    private Map<String, Object> toSummary(AiCompanyAnalysis a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("stockCode", a.getStockCode());
        m.put("stockName", a.getStockName());
        m.put("providerType", a.getProviderType());
        m.put("overallScore", a.getOverallScore());
        m.put("decision", a.getDecision());
        m.put("validUntil", a.getValidUntil());
        return m;
    }

    private Map<String, Object> toDetail(AiCompanyAnalysis a) {
        Map<String, Object> m = toSummary(a);
        m.put("buyScore", a.getBuyScore());
        m.put("riskScore", a.getRiskScore());
        m.put("newsScore", a.getNewsScore());
        m.put("disclosureScore", a.getDisclosureScore());
        m.put("confidence", a.getConfidence());
        m.put("summary", a.getSummary());
        m.put("buyReasons", a.getBuyReasons());
        m.put("riskReasons", a.getRiskReasons());
        m.put("watchPoints", a.getWatchPoints());
        m.put("keyNewsSummary", a.getKeyNewsSummary());
        m.put("keyDisclosureSummary", a.getKeyDisclosureSummary());
        m.put("themeSummary", a.getThemeSummary());
        m.put("validFrom", a.getValidFrom());
        return m;
    }
}
