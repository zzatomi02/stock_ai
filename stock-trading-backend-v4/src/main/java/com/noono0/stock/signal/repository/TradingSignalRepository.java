package com.noono0.stock.signal.repository;

import com.noono0.stock.signal.domain.TradingSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TradingSignalRepository extends JpaRepository<TradingSignal, Long> {
    boolean existsByNewsArticleIdAndSide(Long newsArticleId, String side);

    List<TradingSignal> findByUserIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            String userId, String status, LocalDateTime after);

    List<TradingSignal> findByStatusAndSideAndCreatedAtAfterOrderByFinalScoreDesc(
            String status, String side, LocalDateTime after);

    List<TradingSignal> findByStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            String status, LocalDateTime after);

    List<TradingSignal> findByStatusAndSignalGradeAndCreatedAtAfterOrderByFinalScoreDesc(
            String status, String signalGrade, LocalDateTime after);

    @Query(
            """
            SELECT s FROM TradingSignal s
            WHERE s.status = :status AND s.createdAt >= :after
            ORDER BY s.finalScore DESC
            """)
    List<TradingSignal> findTodayCandidates(
            @Param("status") String status, @Param("after") LocalDateTime after);
}
