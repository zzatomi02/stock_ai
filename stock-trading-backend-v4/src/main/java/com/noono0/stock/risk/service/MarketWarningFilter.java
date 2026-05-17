package com.noono0.stock.risk.service;

import com.noono0.stock.risk.domain.StockWarningStatus;
import com.noono0.stock.risk.domain.enums.RiskEventType;
import com.noono0.stock.risk.domain.enums.StockWarningType;
import com.noono0.stock.risk.dto.WarningFilterResult;
import com.noono0.stock.risk.repository.StockWarningStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * 투자주의·경고·거래정지 등 종목 경보 필터.
 *
 * <p>투자경고/위험/거래정지 → 매수 금지. 투자주의 → riskScore 가산(감점).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketWarningFilter {

    private final StockWarningStatusRepository warningRepository;
    private final RiskEventService riskEventService;

    @Transactional(readOnly = true)
    public WarningFilterResult evaluate(String stockCode) {
        if (stockCode == null || stockCode.isBlank()) {
            return WarningFilterResult.clear();
        }
        Optional<StockWarningStatus> opt = warningRepository.findByStockCodeAndActiveTrue(stockCode.trim());
        if (opt.isEmpty()) {
            return WarningFilterResult.clear();
        }
        StockWarningStatus w = opt.get();
        StockWarningType type;
        try {
            type = StockWarningType.valueOf(w.getWarningType());
        } catch (IllegalArgumentException e) {
            type = StockWarningType.INVESTMENT_CAUTION;
        }

        String reason = w.getRiskReason() != null ? w.getRiskReason() : type.label();
        int scoreAdj = w.getRiskScoreAdjustment() != 0 ? w.getRiskScoreAdjustment() : type.defaultRiskScorePenalty();

        if (w.isBuyBlocked() || type.defaultBuyBlocked()) {
            log.info("[WARNING-FILTER] 매수 차단 {} — {} ({})", stockCode, type.name(), reason);
            riskEventService.record(
                    RiskEventType.BUY_BLOCKED_WARNING,
                    "HIGH",
                    reason,
                    stockCode,
                    null,
                    Map.of("warningType", type.name(), "riskScoreAdjustment", scoreAdj));
            return WarningFilterResult.blocked(type.name(), reason, scoreAdj);
        }

        return WarningFilterResult.caution(type.name(), reason, scoreAdj);
    }

    @Transactional(readOnly = true)
    public boolean isBuyBlocked(String stockCode) {
        return !evaluate(stockCode).buyAllowed();
    }
}
