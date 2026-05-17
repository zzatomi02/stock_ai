package com.noono0.stock.ai.prompt.bootstrap;

import com.noono0.stock.ai.prompt.domain.AiPromptTemplate;
import com.noono0.stock.ai.prompt.repository.AiPromptTemplateRepository;
import com.noono0.stock.ai.prompt.service.AiPromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(45)
@RequiredArgsConstructor
public class AiPromptTemplateSeeder implements ApplicationRunner {
    private final AiPromptTemplateRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.findByPromptCodeAndIsActiveTrue(AiPromptTemplateService.DEFAULT_NEWS_CODE).isPresent()) {
            return;
        }
        if (repository.count() > 0) {
            return;
        }
        AiPromptTemplate t = new AiPromptTemplate();
        t.setPromptCode(AiPromptTemplateService.DEFAULT_NEWS_CODE);
        t.setPromptName("뉴스 분석 기본");
        t.setPromptType("NEWS");
        t.setModelName("gpt-4o-mini");
        t.setSystemPrompt(
                "당신은 한국 상장기업 뉴스 애널리스트이다. 반드시 JSON 한 객체만 출력한다.\n"
                        + "허용 sentiment: POSITIVE, NEGATIVE, NEUTRAL\n"
                        + "허용 actionHint: WATCH_BUY, WATCH_SELL, HOLD, IGNORE\n"
                        + "허용 eventType: EARNINGS, CONTRACT, MNA, REGULATION, LAWSUIT, PRODUCT, SUPPLY, "
                        + "INVESTMENT, FDA, GOVERNMENT_POLICY, ETC\n"
                        + "키: stockCode, stockName, sentiment, eventType, impactScore(0~100), confidence(0~1), "
                        + "summary, reason(배열), risk(배열), actionHint");
        t.setUserPrompt(
                "[맥락]\n종목코드: {{stockCode}}\n종목명: {{stockName}}\n\n[제목]\n{{title}}\n\n[본문]\n{{summary}}\n\nJSON만 출력.");
        t.setResponseFormat(
                "{\"stockCode\":\"\",\"sentiment\":\"POSITIVE|NEGATIVE|NEUTRAL\",\"impactScore\":0,\"confidence\":0.0,\"actionHint\":\"WATCH_BUY\"}");
        t.setTemperature(new BigDecimal("0.20"));
        t.setMaxTokens(2000);
        t.setIsActive(true);
        t.setVersion(1);
        t.setDescription("DB 관리 기본 뉴스 분석 프롬프트");
        repository.save(t);
    }
}
