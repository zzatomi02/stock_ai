package com.noono0.stock.ai.platform.service;

import com.noono0.stock.ai.platform.domain.AiCompanyAnalysis;
import com.noono0.stock.ai.platform.domain.AiMultiConsensusResult;
import com.noono0.stock.ai.platform.enums.AiAnalysisType;
import com.noono0.stock.ai.platform.repository.AiCompanyAnalysisRepository;
import com.noono0.stock.ai.platform.repository.AiMultiConsensusResultRepository;
import com.noono0.stock.ai.platform.dto.CachedAiCompanyView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 장중 빠른 판단용 — <strong>DB 조회만</strong>, 외부 AI API 호출 없음.
 */
@Service
@RequiredArgsConstructor
public class CachedAiAnalysisQueryService {

    private final AiCompanyAnalysisRepository companyRepository;
    private final AiMultiConsensusResultRepository consensusRepository;

    @Transactional(readOnly = true)
    public Optional<CachedAiCompanyView> getBestForTrading(String stockCode) {
        return companyRepository.findBestValidNow(stockCode).map(CachedAiCompanyView::from);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTradingBundle(String stockCode, LocalDate tradeDate) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stockCode", stockCode);
        m.put("tradeDate", tradeDate.toString());
        m.put("source", "DB_CACHE_ONLY");
        m.put("liveApiCalled", false);

        Optional<CachedAiCompanyView> best = getBestForTrading(stockCode);
        m.put("bestAnalysis", best.orElse(null));

        List<AiCompanyAnalysis> all =
                companyRepository.findByStockCodeAndTradeDateOrderByCreatedAtDesc(stockCode, tradeDate);
        m.put("providerResults", all.stream().map(CachedAiCompanyView::from).toList());

        consensusRepository
                .findTopByStockCodeAndTradeDateAndAnalysisTypeOrderByCreatedAtDesc(
                        stockCode, tradeDate, AiAnalysisType.COMPANY_ANALYSIS.name())
                .ifPresent(c -> m.put("consensus", consensusToMap(c)));

        return m;
    }

    private static Map<String, Object> consensusToMap(AiMultiConsensusResult c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("finalDecision", c.getFinalDecision());
        m.put("finalScore", c.getFinalScore());
        m.put("finalConfidence", c.getFinalConfidence());
        m.put("providerCount", c.getProviderCount());
        m.put("summary", c.getSummary());
        m.put("hasRisk", c.isHasRisk());
        return m;
    }
}
