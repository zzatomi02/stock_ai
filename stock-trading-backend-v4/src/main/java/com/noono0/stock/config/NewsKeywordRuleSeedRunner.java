package com.noono0.stock.config;

import com.noono0.stock.news.domain.NewsKeywordRule;
import com.noono0.stock.news.mapper.NewsKeywordRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * news_keyword_rule 시드. 테이블은 엔티티(ddl-auto) 기준으로 준비된 뒤 1회만 삽입한다.
 */
@Component
@Order(1000)
@RequiredArgsConstructor
public class NewsKeywordRuleSeedRunner implements ApplicationRunner {

    private final NewsKeywordRuleMapper newsKeywordRuleMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (newsKeywordRuleMapper.count() > 0) {
            return;
        }
        insertRule("MOU", 10, "POSITIVE", "업무협약");
        insertRule("합병", 12, "POSITIVE", "합병 이슈");
        insertRule("수주", 15, "POSITIVE", "수주 공시");
        insertRule("흑자전환", 18, "POSITIVE", "흑자 전환");
        insertRule("유상증자", -20, "NEGATIVE", "희석 우려");
        insertRule("횡령", -30, "NEGATIVE", "리스크 이슈");
    }

    private void insertRule(String keyword, int score, String polarity, String description) {
        NewsKeywordRule r = new NewsKeywordRule();
        r.setKeyword(keyword);
        r.setScore(score);
        r.setPolarity(polarity);
        r.setDescription(description);
        r.setActive(true);
        r.setCreatedAt(LocalDateTime.now());
        newsKeywordRuleMapper.insert(r);
    }
}
