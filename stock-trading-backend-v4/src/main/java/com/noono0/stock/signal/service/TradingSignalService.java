package com.noono0.stock.signal.service;

import com.noono0.stock.execution.OrderGateway;
import com.noono0.stock.news.domain.NewsArticle;
import com.noono0.stock.news.repository.NewsArticleJpaRepository;
import com.noono0.stock.signal.domain.TradingSignal;
import com.noono0.stock.signal.repository.TradingSignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TradingSignalService {
    private final TradingSignalRepository signalRepository;
    private final NewsArticleJpaRepository newsArticleJpaRepository;
    private final SignalEngine signalEngine;
    private final OrderGateway orderGateway;

    public Map<String, Object> platformStatus() {
        var phase = orderGateway.currentPhase();
        return Map.of(
                "executionPhase", phase.name(),
                "executionPhaseLabel", phase.labelKo(),
                "observeOnly", orderGateway.allowsSignalOnly(),
                "requiresApproval", orderGateway.requiresHumanApproval(),
                "allowsAutoOrder", phase.allowsAutoOrder());
    }

    public List<TradingSignal> listByGrade(String grade, int limit) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return signalRepository
                .findByStatusAndSignalGradeAndCreatedAtAfterOrderByFinalScoreDesc("CANDIDATE", grade, start)
                .stream()
                .limit(Math.min(100, limit))
                .toList();
    }

    public List<TradingSignal> listToday(String side, String status, int limit) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        String st = StringUtils.hasText(status) ? status : "CANDIDATE";
        int n = Math.min(200, Math.max(1, limit));
        if (StringUtils.hasText(side)) {
            return signalRepository.findByStatusAndSideAndCreatedAtAfterOrderByFinalScoreDesc(st, side, start)
                    .stream()
                    .limit(n)
                    .toList();
        }
        return signalRepository.findTodayCandidates(st, start).stream().limit(n).toList();
    }

    public List<TradingSignal> listRejected(int limit) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return signalRepository.findByStatusAndCreatedAtAfterOrderByCreatedAtDesc("REJECTED", start)
                .stream()
                .limit(Math.min(100, limit))
                .toList();
    }

    @Transactional
    public Optional<TradingSignal> generateFromArticleId(long articleId, String userId) {
        NewsArticle a =
                newsArticleJpaRepository.findById(articleId).orElseThrow(() -> new IllegalArgumentException("기사 없음"));
        return signalEngine.generateFromArticle(a, userId);
    }

    @Transactional
    public int scanRecentArticles(int hours) {
        LocalDateTime after = LocalDateTime.now().minusHours(Math.max(1, hours));
        List<NewsArticle> articles = newsArticleJpaRepository.findByCollectedAtAfterOrderByCollectedAtDesc(after);
        int created = 0;
        for (NewsArticle a : articles) {
            if (signalEngine.generateFromArticle(a, SignalEngine.SYSTEM_USER).isPresent()) {
                created++;
            }
        }
        return created;
    }
}
