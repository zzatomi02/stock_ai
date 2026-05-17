package com.noono0.stock.statistics.service;

import com.noono0.stock.broker.mapper.BrokerOrderAttemptMapper;
import com.noono0.stock.ops.audit.mapper.AuditLogMapper;
import com.noono0.stock.statistics.dto.TradeStatisticsResponse;
import com.noono0.stock.statistics.mapper.TradeStatisticsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TradeStatisticsService {
    private final TradeStatisticsMapper mapper;
    private final AuditLogMapper auditLogMapper;
    private final BrokerOrderAttemptMapper brokerOrderAttemptMapper;

    public TradeStatisticsResponse getStatistics(String range, LocalDate fromDate, LocalDate toDate, String keyword) {
        LocalDate today = LocalDate.now();
        LocalDate from = fromDate;
        LocalDate to = toDate;
        if (from == null || to == null) {
            switch (range == null ? "today" : range) {
                case "week" -> { from = today.minusDays(6); to = today; }
                case "month" -> { from = today.minusMonths(1).plusDays(1); to = today; }
                default -> { from = today; to = today; }
            }
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusSeconds(1);
        return new TradeStatisticsResponse(
                mapper.findTradeSummary(start, end, keyword),
                mapper.findTradeTimeline(start, end),
                mapper.findKeywordPerformance(start, end)
        );
    }

    public Map<String, Object> healthSnapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tradeExecutionCount", mapper.countTradeExecutions());
        m.put("auditLogCount", auditLogMapper.count());
        m.put("brokerOrderAttemptCount", brokerOrderAttemptMapper.count());
        return m;
    }
}
