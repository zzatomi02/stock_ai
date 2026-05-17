package com.noono0.stock.ai.platform.service;

import com.noono0.stock.ai.platform.repository.AiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiProviderPerformanceService {

    private final AiUsageLogRepository usageLogRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> statsSince(LocalDateTime since) {
        return usageLogRepository.summarizeByProviderSince(since).stream()
                .map(
                        row -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("providerType", row[0]);
                            m.put("totalCalls", row[1]);
                            m.put("successCalls", row[2]);
                            m.put("totalInputTokens", row[3]);
                            m.put("totalOutputTokens", row[4]);
                            m.put("totalEstimatedCost", row[5]);
                            long total = row[1] != null ? ((Number) row[1]).longValue() : 0;
                            long ok = row[2] != null ? ((Number) row[2]).longValue() : 0;
                            m.put("successRate", total > 0 ? (double) ok / total : 0);
                            return m;
                        })
                .toList();
    }
}
