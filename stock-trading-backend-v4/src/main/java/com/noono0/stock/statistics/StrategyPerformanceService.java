package com.noono0.stock.statistics;

import com.noono0.stock.broker.domain.BrokerOrderAttempt;
import com.noono0.stock.broker.mapper.BrokerOrderAttemptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 전략 성과 통계 — broker_order_attempt·strategy_signal 기반 집계(1차).
 *
 * <p>실현 손익·승률 정밀화는 trade_execution 연동 후 확장.
 */
@Service
@RequiredArgsConstructor
public class StrategyPerformanceService {

    private final BrokerOrderAttemptMapper orderMapper;

    public Map<String, Object> summary(LocalDate from, LocalDate to) {
        List<BrokerOrderAttempt> recent = orderMapper.findRecent(500);
        long buys =
                recent.stream()
                        .filter(o -> "BUY".equalsIgnoreCase(o.getSide()))
                        .filter(o -> inRange(o, from, to))
                        .count();
        long sells =
                recent.stream()
                        .filter(o -> "SELL".equalsIgnoreCase(o.getSide()))
                        .filter(o -> inRange(o, from, to))
                        .count();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("from", from.toString());
        m.put("to", to.toString());
        m.put("totalTrades", buys + sells);
        m.put("buyCount", buys);
        m.put("sellCount", sells);
        m.put("winRate", null);
        m.put("avgReturnRate", null);
        m.put("mdd", null);
        m.put("profitFactor", null);
        m.put(
                "note",
                "승률·MDD·손익비는 trade_execution·백테스트 엔진 연동 후 계산됩니다.");
        return m;
    }

    private static boolean inRange(BrokerOrderAttempt o, LocalDate from, LocalDate to) {
        if (o.getCreatedAt() == null) {
            return false;
        }
        LocalDate d = o.getCreatedAt().toLocalDate();
        return !d.isBefore(from) && !d.isAfter(to);
    }
}
