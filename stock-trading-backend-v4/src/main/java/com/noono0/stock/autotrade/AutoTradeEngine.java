package com.noono0.stock.autotrade;

import com.noono0.stock.broker.domain.BrokerOrderAttempt;
import com.noono0.stock.broker.mapper.BrokerOrderAttemptMapper;
import com.noono0.stock.execution.OrderGateway;
import com.noono0.stock.integration.kis.config.KisProperties;
import com.noono0.stock.integration.kis.service.KisBrokerService;
import com.noono0.stock.ops.schedule.ScheduleMonitor;
import com.noono0.stock.ops.trading.TradingControlService;
import com.noono0.stock.strategy.domain.Strategy;
import com.noono0.stock.strategy.mapper.StrategyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 조건 충족 시 모의 매수 샘플. 중복 clientOrderKey·재진입 쿨다운·긴급중지 반영.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.auto-trade.enabled", havingValue = "true")
public class AutoTradeEngine {
    private final StrategyMapper strategyMapper;
    private final TradingControlService tradingControlService;
    private final KisBrokerService kisBrokerService;
    private final BrokerOrderAttemptMapper brokerOrderAttemptMapper;
    private final KisProperties kisProperties;
    private final NotificationDispatcher notificationDispatcher;
    private final ScheduleMonitor scheduleMonitor;
    private final OrderGateway orderGateway;

    @Value("${app.auto-trade.demo-stock:005930}")
    private String demoStock;

    @Value("${app.auto-trade.demo-qty:1}")
    private int demoQty;

    @Scheduled(fixedDelayString = "${app.auto-trade.scan-interval-ms:60000}")
    public void tick() {
        scheduleMonitor.touch("auto-trade-scan");
        if (tradingControlService.isEmergencyStop()) {
            return;
        }
        try {
            orderGateway.assertAutoTradeAllowed();
        } catch (Exception e) {
            log.debug("【AUTO-TRADE】 단계 제한: {}", e.getMessage());
            return;
        }
        if (!StringUtils.hasText(kisProperties.getAccountNo())) {
            return;
        }
        for (Strategy s : strategyMapper.selectAll()) {
            if (!Boolean.TRUE.equals(s.getEnabled()) || !Boolean.TRUE.equals(s.getAutoTradeEnabled())) {
                continue;
            }
            String clientKey = "A-" + s.getId() + "-" + demoStock + "-" + LocalDateTime.now().toLocalDate();
            if (brokerOrderAttemptMapper.countByClientOrderKey(clientKey) > 0) {
                continue;
            }
            if (reentryBlocked(s, demoStock)) {
                continue;
            }
            try {
                var res = kisBrokerService.orderBuyMarket(null, demoStock, demoQty);
                BrokerOrderAttempt b = new BrokerOrderAttempt();
                b.setClientOrderKey(clientKey);
                b.setStockCode(demoStock);
                b.setSide("BUY");
                b.setQuantity(demoQty);
                b.setMode("paper");
                b.setRawResponse(res.toString());
                b.setReasoning("autotrade: strategyId=" + s.getId() + ", paper 시장가 매수(샘플), 종목=" + demoStock);
                b.setCreatedAt(LocalDateTime.now());
                brokerOrderAttemptMapper.insert(b);
                log.warn(
                        "【AUTO-TRADE】 ★ 모의 시장가 매수 기록 완료 ★ strategyId={} stock={} qty={} clientKey={} id={}",
                        s.getId(),
                        demoStock,
                        demoQty,
                        clientKey,
                        b.getId());
                notificationDispatcher.notify("자동매매(모의) 시도: " + clientKey);
            } catch (Exception e) {
                log.warn("【AUTO-TRADE】 자동매매 실패: {}", e.getMessage());
            }
            return;
        }
    }

    private boolean reentryBlocked(Strategy s, String stock) {
        int cool = s.getReentryCooldownMinutes() != null ? s.getReentryCooldownMinutes() : 30;
        return brokerOrderAttemptMapper.countByStockCodeAndCreatedAtAfter(
                       stock, LocalDateTime.now().minusMinutes(cool))
                > 0;
    }
}
