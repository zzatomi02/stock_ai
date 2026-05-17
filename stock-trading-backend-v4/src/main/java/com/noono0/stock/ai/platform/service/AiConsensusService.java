package com.noono0.stock.ai.platform.service;

import com.noono0.stock.ai.platform.domain.AiCompanyAnalysis;
import com.noono0.stock.ai.platform.domain.AiMultiConsensusResult;
import com.noono0.stock.ai.platform.enums.AiAnalysisType;
import com.noono0.stock.ai.platform.enums.AiTargetType;
import com.noono0.stock.ai.platform.repository.AiCompanyAnalysisRepository;
import com.noono0.stock.ai.platform.repository.AiMultiConsensusResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiConsensusService {

    private final AiCompanyAnalysisRepository companyRepository;
    private final AiMultiConsensusResultRepository consensusRepository;

    @Transactional
    public AiMultiConsensusResult buildCompanyConsensus(String stockCode, LocalDate tradeDate) {
        List<AiCompanyAnalysis> rows =
                companyRepository.findByStockCodeAndTradeDateOrderByCreatedAtDesc(stockCode, tradeDate);
        if (rows.isEmpty()) {
            return null;
        }

        int pass = 0, watch = 0, avoid = 0, risk = 0;
        BigDecimal scoreSum = BigDecimal.ZERO;
        BigDecimal confSum = BigDecimal.ZERO;
        for (AiCompanyAnalysis a : rows) {
            String d = a.getDecision() != null ? a.getDecision() : "WATCH";
            if (d.contains("BUY")) pass++;
            else if (d.contains("AVOID") || d.contains("SELL")) avoid++;
            else if (a.getRiskScore() != null && a.getRiskScore().doubleValue() >= 60) risk++;
            else watch++;
            if (a.getOverallScore() != null) scoreSum = scoreSum.add(a.getOverallScore());
            if (a.getConfidence() != null) confSum = confSum.add(a.getConfidence());
        }
        int n = rows.size();
        BigDecimal avgScore = scoreSum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        BigDecimal avgConf = confSum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);

        String finalDecision;
        if (pass >= n / 2 + 1) finalDecision = "BUY_CANDIDATE";
        else if (avoid >= n / 2) finalDecision = "AVOID";
        else if (risk >= 2) finalDecision = "HIGH_RISK";
        else finalDecision = "WATCH";

        AiMultiConsensusResult c = new AiMultiConsensusResult();
        c.setAnalysisType(AiAnalysisType.COMPANY_ANALYSIS.name());
        c.setTargetType(AiTargetType.STOCK.name());
        c.setStockCode(stockCode);
        c.setTradeDate(tradeDate);
        c.setFinalDecision(finalDecision);
        c.setFinalScore(avgScore);
        c.setFinalConfidence(avgConf);
        c.setConsensusMethod("WEIGHTED_VOTE_V1");
        c.setProviderCount(n);
        c.setPassCount(pass);
        c.setWatchCount(watch);
        c.setAvoidCount(avoid);
        c.setRiskCount(risk);
        c.setDisagreementCount(Math.max(pass, Math.max(avoid, watch)) > 1 ? 1 : 0);
        c.setHasRisk(risk > 0);
        c.setSummary(
                "AI "
                        + n
                        + "건 합의 — "
                        + finalDecision
                        + " (avgScore="
                        + avgScore
                        + ")");
        return consensusRepository.save(c);
    }
}
