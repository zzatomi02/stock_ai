package com.noono0.stock.risk.config;

import com.noono0.stock.risk.domain.enums.StockWarningType;
import com.noono0.stock.risk.dto.StockWarningUpsertRequest;
import com.noono0.stock.risk.service.MarketWarningAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 개발용 더미 경보 종목 */
@Component
@Order(20)
@RequiredArgsConstructor
public class StockWarningSeedRunner implements ApplicationRunner {

    private final MarketWarningAdminService warningAdminService;

    @Override
    public void run(ApplicationArguments args) {
        seed("000000", "더미투자주의", StockWarningType.INVESTMENT_CAUTION, "테스트: 투자주의 — 점수 감점");
        seed("999999", "더미거래정지", StockWarningType.TRADING_HALT, "테스트: 거래정지 — 매수 금지");
    }

    private void seed(String code, String name, StockWarningType type, String reason) {
        try {
            warningAdminService.upsert(
                    new StockWarningUpsertRequest(
                            code, name, type.name(), type.defaultRiskScorePenalty(), null, reason));
        } catch (Exception ignored) {
            // 이미 존재
        }
    }
}
