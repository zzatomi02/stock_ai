package com.noono0.stock.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.news.service.NewsKeywordService;
import com.noono0.stock.order.domain.OrderReason;
import com.noono0.stock.order.repository.OrderReasonRepository;
import com.noono0.stock.signal.domain.TradingSignal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderReasonService {
    private final OrderReasonRepository repository;
    private final ObjectMapper objectMapper;
    private final NewsKeywordService newsKeywordService;

    @Transactional
    public OrderReason saveForSignal(TradingSignal signal, String reasonType) {
        OrderReason r = new OrderReason();
        r.setSignalId(signal.getId());
        r.setUserId(signal.getUserId());
        r.setStockCode(signal.getStockCode());
        r.setReasonType(reasonType != null ? reasonType : "SIGNAL_CANDIDATE");
        r.setSummary(buildSummary(signal));
        r.setAiSummary(signal.getLlmSummary());
        r.setNewsReason(
                "AI 분석: " + nullSafe(signal.getLlmSentiment()) + ", impact=" + signal.getAiScore()
                        + ", confidence=" + signal.getLlmConfidence());
        if (signal.getNewsArticleId() != null) {
            var matches = newsKeywordService.matchHistory(signal.getNewsArticleId());
            String kw =
                    matches.stream().map(m -> m.getKeyword() + "(" + m.getKeywordType() + ")").collect(Collectors.joining(", "));
            r.setKeywordReason(kw.isBlank() ? "키워드 매칭 없음" : kw);
        }
        r.setMarketReason("시장 분위기 점수는 SignalEngine에서 반영");
        r.setRiskReason(signal.getRejectedReason());
        r.setStrategyReason("관찰 모드: 주문 없음, 후보만 저장");
        try {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("finalScore", signal.getFinalScore());
            json.put("grade", signal.getSignalGrade());
            json.put("reasonSnapshot", signal.getReasonSnapshot());
            r.setReasonJson(objectMapper.writeValueAsString(json));
        } catch (Exception e) {
            r.setReasonJson("{}");
        }
        return repository.save(r);
    }

    public Optional<OrderReason> findBySignalId(long signalId) {
        return repository.findBySignalId(signalId);
    }

    private static String buildSummary(TradingSignal s) {
        return (s.getStockName() != null ? s.getStockName() : s.getStockCode())
                + " "
                + s.getSide()
                + " "
                + (s.getStatus() != null ? s.getStatus() : "")
                + " 최종점수 "
                + s.getFinalScore();
    }

    private static String nullSafe(String s) {
        return s == null ? "-" : s;
    }
}
