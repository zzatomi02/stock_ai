package com.noono0.stock.config;

import com.noono0.stock.collect.domain.NewsSearchKeyword;
import com.noono0.stock.collect.repository.NewsSearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 네이버 뉴스 검색 키워드 시드 (OR + SINGLE) */
@Component
@Order(998)
@RequiredArgsConstructor
public class NewsSearchKeywordSeedRunner implements ApplicationRunner {
    private final NewsSearchKeywordRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        seed(
                "증시 OR 코스피 OR 코스닥 OR 환율 OR 금리",
                "MARKET",
                "OR",
                1,
                180,
                50,
                "시장 분위기·거시 레이더");
        seed(
                "거래정지 OR 투자주의 OR 관리종목 OR 상장폐지",
                "BAD_EVENT",
                "OR",
                1,
                300,
                30,
                "위험·규제 이벤트 레이더");
        seed("실적 호재 OR 수주 OR 목표가 상향", "GOOD_EVENT", "OR", 2, 300, 30, "호재 레이더");
        seed("반도체", "THEME", "SINGLE", 3, 600, 20, "반도체 테마");
        seed("HBM", "THEME", "SINGLE", 3, 600, 20, "HBM 테마");
        seed("2차전지", "THEME", "SINGLE", 4, 900, 15, "2차전지 테마");
        seed("바이오", "THEME", "SINGLE", 4, 900, 15, "바이오 테마");
        seed("로봇", "THEME", "SINGLE", 4, 900, 15, "로봇 테마");
    }

    private void seed(
            String keyword,
            String group,
            String searchType,
            int priority,
            int intervalSeconds,
            int displayCount,
            String description) {
        NewsSearchKeyword k = new NewsSearchKeyword();
        k.setKeyword(keyword);
        k.setKeywordGroup(group);
        k.setSearchType(searchType);
        k.setPriority(priority);
        k.setIntervalSeconds(intervalSeconds);
        k.setDisplayCount(displayCount);
        k.setSortType("date");
        k.setEnabled(true);
        k.setDescription(description);
        k.setCreatedAt(LocalDateTime.now());
        k.setUpdatedAt(LocalDateTime.now());
        repository.save(k);
    }
}
