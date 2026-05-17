package com.noono0.stock.ai.platform.bootstrap;

import com.noono0.stock.ai.platform.domain.AiAnalysisPromptTemplate;
import com.noono0.stock.ai.platform.domain.AiModelConfig;
import com.noono0.stock.ai.platform.domain.AiProviderConfig;
import com.noono0.stock.ai.platform.enums.AiAnalysisType;
import com.noono0.stock.ai.platform.enums.AiProviderType;
import com.noono0.stock.ai.platform.repository.AiAnalysisPromptTemplateRepository;
import com.noono0.stock.ai.platform.repository.AiModelConfigRepository;
import com.noono0.stock.ai.platform.repository.AiProviderConfigRepository;
import com.noono0.stock.ai.platform.config.AiPlatformProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(15)
@RequiredArgsConstructor
public class AiPlatformSeedRunner implements ApplicationRunner {

    private final AiProviderConfigRepository providerRepository;
    private final AiModelConfigRepository modelRepository;
    private final AiAnalysisPromptTemplateRepository templateRepository;
    private final AiPlatformProperties platformProperties;

    @Override
    public void run(ApplicationArguments args) {
        seedProvider(AiProviderType.OPENAI, "OpenAI", "OPENAI_API_KEY", "1.20", true);
        seedProvider(AiProviderType.GEMINI, "Google Gemini", "GEMINI_API_KEY", "1.10", true);
        seedProvider(AiProviderType.CLAUDE, "Anthropic Claude", "CLAUDE_API_KEY", "1.00", true);
        seedProvider(AiProviderType.PERPLEXITY, "Perplexity", "PERPLEXITY_API_KEY", "0.80", false);
        seedProvider(AiProviderType.LOCAL_LLM, "Local LLM", null, "0.50", false);

        String model = platformProperties.getDefaultOpenAiModel();
        seedModel(AiProviderType.OPENAI, model, AiAnalysisType.COMPANY_ANALYSIS, "1.20");
        seedModel(AiProviderType.GEMINI, "gemini-1.5-flash", AiAnalysisType.COMPANY_ANALYSIS, "1.10");
        seedModel(AiProviderType.CLAUDE, "claude-3-5-sonnet-latest", AiAnalysisType.COMPANY_ANALYSIS, "1.00");

        if (templateRepository
                .findFirstByAnalysisTypeAndActiveTrueOrderByUpdatedAtDesc(
                        AiAnalysisType.COMPANY_ANALYSIS.name())
                .isEmpty()) {
            templateRepository.save(companyTemplate());
        }
    }

    private void seedProvider(
            AiProviderType type, String name, String env, String weight, boolean enabled) {
        providerRepository.findByProviderType(type.name()).orElseGet(() -> {
            AiProviderConfig p = new AiProviderConfig();
            p.setProviderType(type.name());
            p.setProviderName(name);
            p.setApiKeyEnvName(env);
            p.setEnabled(enabled);
            p.setDefaultWeight(new BigDecimal(weight));
            return providerRepository.save(p);
        });
    }

    private void seedModel(AiProviderType provider, String model, AiAnalysisType analysis, String weight) {
        var existing =
                modelRepository.findByProviderTypeAndAnalysisTypeAndEnabledTrue(
                        provider.name(), analysis.name());
        if (!existing.isEmpty()) {
            return;
        }
        AiModelConfig m = new AiModelConfig();
        m.setProviderType(provider.name());
        m.setModelName(model);
        m.setAnalysisType(analysis.name());
        m.setEnabled(true);
        m.setWeight(new BigDecimal(weight));
        modelRepository.save(m);
    }

    private static AiAnalysisPromptTemplate companyTemplate() {
        AiAnalysisPromptTemplate t = new AiAnalysisPromptTemplate();
        t.setAnalysisType(AiAnalysisType.COMPANY_ANALYSIS.name());
        t.setTemplateName("기업 종합 분석 v1");
        t.setPromptVersion("v1.0.0");
        t.setSystemPrompt(
                """
                너는 한국 주식 단기매매/스윙매매 보조 애널리스트다.
                너는 매수/매도 주문을 직접 지시하지 않는다.
                입력된 뉴스, 공시, 수급, 가격, 리스크 정보를 바탕으로 기업의 단기 매매 관점 점수와 리스크를 구조화된 JSON으로 분석한다.
                과장하지 말고, 불확실하면 confidence를 낮춰라.
                위험 공시나 악재가 있으면 명확히 표시하라.""");
        t.setUserPromptTemplate(
                """
                아래 종목을 단기매매 관점에서 분석해줘.

                종목코드: {{stockCode}}
                종목명: {{stockName}}
                섹터: {{sectorName}}
                거래일: {{tradeDate}}
                현재 시장상태: {{marketCondition}}

                최근 가격 요약:
                {{priceSummary}}

                최근 수급 요약:
                {{supplySummary}}

                최근 뉴스:
                {{recentNews}}

                최근 공시:
                {{recentDisclosures}}

                기술적 요약:
                {{technicalSummary}}

                리스크 요약:
                {{riskSummary}}

                아래 JSON 형식으로만 응답해줘.
                {
                  "overallScore": 0,
                  "buyScore": 0,
                  "riskScore": 0,
                  "newsScore": 0,
                  "disclosureScore": 0,
                  "themeScore": 0,
                  "supplyScore": 0,
                  "decision": "WATCH",
                  "confidence": 0.0,
                  "summary": "",
                  "buyReasons": [],
                  "riskReasons": [],
                  "watchPoints": [],
                  "keyNewsSummary": "",
                  "keyDisclosureSummary": "",
                  "themeSummary": ""
                }""");
        t.setActive(true);
        t.setDescription("장전/장마감 배치용 기업 종합 분석");
        return t;
    }
}
