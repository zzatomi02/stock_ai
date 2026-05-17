package com.noono0.stock.config;

import com.noono0.stock.news.domain.NewsKeyword;
import com.noono0.stock.news.repository.NewsKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(999)
@RequiredArgsConstructor
public class NewsKeywordSeedRunner implements ApplicationRunner {
    private final NewsKeywordRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        String[][] positive = {
            {"수주", "15"}, {"공급계약", "15"}, {"실적 개선", "12"}, {"흑자전환", "18"}, {"목표가 상향", "10"},
            {"자사주 매입", "10"}, {"배당 확대", "8"}, {"MOU", "10"}, {"기술수출", "12"}, {"FDA 승인", "14"},
            {"정부 지원", "10"}, {"신규 투자", "10"}, {"공장 증설", "10"}, {"HBM", "12"}, {"방산 수출", "12"},
        };
        String[][] negative = {
            {"실적 부진", "12"}, {"적자전환", "18"}, {"영업손실", "15"}, {"소송", "15"}, {"횡령", "25"},
            {"배임", "25"}, {"압수수색", "20"}, {"상장폐지", "30"}, {"거래정지", "30"}, {"유상증자", "20"},
            {"전환사채", "15"}, {"CB 발행", "15"}, {"불성실공시", "18"}, {"목표가 하향", "12"}, {"공급계약 해지", "15"},
            {"리콜", "12"}, {"규제", "12"}, {"과징금", "15"}, {"제재", "15"}, {"감사의견 거절", "20"},
        };
        for (String[] row : positive) {
            save(row[0], "POSITIVE", Integer.parseInt(row[1]));
        }
        for (String[] row : negative) {
            save(row[0], "NEGATIVE", Integer.parseInt(row[1]));
        }
    }

    private void save(String keyword, String type, int weight) {
        NewsKeyword k = new NewsKeyword();
        k.setKeyword(keyword);
        k.setKeywordType(type);
        k.setWeight(weight);
        k.setIsActive(true);
        k.setCreatedAt(LocalDateTime.now());
        k.setUpdatedAt(LocalDateTime.now());
        repository.save(k);
    }
}
