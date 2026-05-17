package com.noono0.stock.llm.bootstrap;

import com.noono0.stock.llm.domain.LlmQuestionTemplate;
import com.noono0.stock.llm.repository.LlmQuestionTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * DB가 비어 있을 때 기본 뉴스 LLM 질문 템플릿 1건 삽입.
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class LlmDefaultTemplateSeeder implements ApplicationRunner {
    private static final String DEFAULT_SYSTEM =
            "당신은 한국 상장기업 뉴스 애널리스트이다. 반드시 JSON 한 객체만 출력한다.\n"
                    + "허용 sentiment: POSITIVE, NEGATIVE, NEUTRAL\n"
                    + "허용 actionHint: WATCH_BUY, WATCH_SELL, HOLD, IGNORE\n"
                    + "허용 eventType: EARNINGS, CONTRACT, MNA, REGULATION, LAWSUIT, PRODUCT, SUPPLY, "
                    + "INVESTMENT, FDA, GOVERNMENT_POLICY, ETC\n"
                    + "키: stockCode, stockName, sentiment, eventType, impactScore(0~100), confidence(0~1), "
                    + "summary, reason(문자열 배열), risk(문자열 배열), actionHint\n"
                    + "단순 호재/악재 한마디가 아니라 근거와 리스크를 분리한다.";

    private static final String DEFAULT_USER =
            "[맥락]\n"
                    + "종목코드: {{stockCode}}\n"
                    + "종목명: {{stockName}}\n"
                    + "\n"
                    + "[뉴스 제목]\n"
                    + "{{title}}\n"
                    + "\n"
                    + "[요약/본문]\n"
                    + "{{summary}}\n"
                    + "\n"
                    + "단기(수일) 관점 impactScore와 actionHint를 산출한다. JSON만 출력.";

    private final LlmQuestionTemplateRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        LlmQuestionTemplate t = new LlmQuestionTemplate();
        t.setName("default-ko-news");
        t.setSystemPrompt(DEFAULT_SYSTEM);
        t.setUserMessageTemplate(DEFAULT_USER);
        t.setOpenaiModel("gpt-4o-mini");
        t.setTemperature(0.2);
        t.setDefaultTemplate(true);
        repository.save(t);
        log.warn("【LLM-TEMPLATE】 ★ 기본 질문 템플릿 시드 삽입 ★ name={} id={}", t.getName(), t.getId());
    }
}
