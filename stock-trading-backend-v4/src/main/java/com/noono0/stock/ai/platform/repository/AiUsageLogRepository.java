package com.noono0.stock.ai.platform.repository;

import com.noono0.stock.ai.platform.domain.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    @Query(
            """
            SELECT u.providerType,
                   COUNT(u),
                   SUM(CASE WHEN u.success = true THEN 1 ELSE 0 END),
                   COALESCE(SUM(u.inputTokens), 0),
                   COALESCE(SUM(u.outputTokens), 0),
                   COALESCE(SUM(u.estimatedCost), 0)
            FROM AiUsageLog u
            WHERE u.createdAt >= :since
            GROUP BY u.providerType
            ORDER BY COUNT(u) DESC
            """)
    List<Object[]> summarizeByProviderSince(@Param("since") LocalDateTime since);
}
