package com.noono0.stock.market.service;

import com.noono0.stock.market.domain.MarketMoodSnapshot;
import com.noono0.stock.market.repository.MarketMoodSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** KIS 지수 연동 전까지 중립(50) 스냅샷. 향후 KOSPI/KOSDAQ·수급 반영. */
@Service
@RequiredArgsConstructor
public class MarketMoodService {
    private final MarketMoodSnapshotRepository repository;

    public MarketMoodSnapshot currentOrDefault() {
        return repository
                .findTopByOrderByCreatedAtDesc()
                .orElseGet(this::neutralSnapshot);
    }

    public int currentScore() {
        return currentOrDefault().getMoodScore();
    }

    public String labelForScore(int score) {
        if (score <= 30) return "매우 약세";
        if (score <= 45) return "약세";
        if (score <= 55) return "중립";
        if (score <= 70) return "강세";
        return "매우 강세";
    }

    private MarketMoodSnapshot neutralSnapshot() {
        MarketMoodSnapshot s = new MarketMoodSnapshot();
        s.setMoodScore(50);
        s.setMoodLabel("중립");
        s.setCreatedAt(LocalDateTime.now());
        return s;
    }
}
