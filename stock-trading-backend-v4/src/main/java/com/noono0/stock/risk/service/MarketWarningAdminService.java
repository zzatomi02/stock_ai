package com.noono0.stock.risk.service;

import com.noono0.stock.risk.domain.MarketWarningHistory;
import com.noono0.stock.risk.domain.StockWarningStatus;
import com.noono0.stock.risk.domain.enums.StockWarningType;
import com.noono0.stock.risk.dto.StockWarningUpsertRequest;
import com.noono0.stock.risk.repository.MarketWarningHistoryRepository;
import com.noono0.stock.risk.repository.StockWarningStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketWarningAdminService {

    private final StockWarningStatusRepository statusRepository;
    private final MarketWarningHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public List<StockWarningStatus> listActive() {
        return statusRepository.findByActiveTrueOrderByStockCodeAsc();
    }

    @Transactional
    public StockWarningStatus upsert(StockWarningUpsertRequest req) {
        StockWarningType type = StockWarningType.valueOf(req.warningType());
        StockWarningStatus entity =
                statusRepository
                        .findByStockCode(req.stockCode())
                        .orElseGet(StockWarningStatus::new);
        boolean isNew = entity.getId() == null;
        entity.setStockCode(req.stockCode());
        entity.setStockName(req.stockName());
        entity.setWarningType(type.name());
        entity.setRiskScoreAdjustment(
                req.riskScoreAdjustment() != null ? req.riskScoreAdjustment() : type.defaultRiskScorePenalty());
        entity.setBuyBlocked(req.buyBlocked() != null ? req.buyBlocked() : type.defaultBuyBlocked());
        entity.setRiskReason(req.riskReason() != null ? req.riskReason() : type.label());
        entity.setActive(true);
        StockWarningStatus saved = statusRepository.save(entity);

        MarketWarningHistory h = new MarketWarningHistory();
        h.setStockCode(saved.getStockCode());
        h.setWarningType(saved.getWarningType());
        h.setAction(isNew ? "ADDED" : "UPDATED");
        h.setRiskReason(saved.getRiskReason());
        historyRepository.save(h);
        return saved;
    }

    @Transactional
    public void deactivate(String stockCode) {
        statusRepository.findByStockCodeAndActiveTrue(stockCode).ifPresent(s -> {
            s.setActive(false);
            statusRepository.save(s);
            MarketWarningHistory h = new MarketWarningHistory();
            h.setStockCode(stockCode);
            h.setWarningType(s.getWarningType());
            h.setAction("REMOVED");
            h.setRiskReason(s.getRiskReason());
            historyRepository.save(h);
        });
    }
}
