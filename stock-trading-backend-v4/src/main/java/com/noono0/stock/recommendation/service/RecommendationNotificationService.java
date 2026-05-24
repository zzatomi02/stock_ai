package com.noono0.stock.recommendation.service;

import com.noono0.stock.autotrade.NotificationDispatcher;
import com.noono0.stock.execution.runtime.ExecutionRuntimeConfig;
import com.noono0.stock.execution.runtime.ExecutionRuntimeService;
import com.noono0.stock.recommendation.domain.StockRecommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationNotificationService {

    private final NotificationDispatcher notificationDispatcher;
    private final ExecutionRuntimeService executionRuntimeService;
    private final RestClient restClient;

    public void notifyNewRecommendation(StockRecommendation recommendation) {
        ExecutionRuntimeConfig.NotificationChannels notificationChannels =
                executionRuntimeService.get().getNotification();
        if (!executionRuntimeService.get().getRecommendation().isNotifyOnNew()) {
            return;
        }
        String messageBody = formatMessage(recommendation);
        if (notificationChannels.isTelegram()
                || notificationChannels.isDiscord()
                || notificationChannels.isEmail()) {
            notificationDispatcher.notify(messageBody);
        }
        if (notificationChannels.isKakao() && StringUtils.hasText(notificationChannels.getKakaoWebhookUrl())) {
            postWebhook(notificationChannels.getKakaoWebhookUrl(), messageBody, "kakao");
        }
        if (notificationChannels.isSms() && StringUtils.hasText(notificationChannels.getSmsWebhookUrl())) {
            postWebhook(notificationChannels.getSmsWebhookUrl(), messageBody, "sms");
        }
    }

    private void postWebhook(String url, String text, String channel) {
        try {
            String json =
                    "{\"text\":\""
                            + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                            + "\"}";
            restClient
                    .post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            log.warn("[{}-webhook] {}", channel, exception.getMessage());
        }
    }

    static String formatMessage(StockRecommendation rec) {
        String score =
                rec.getBestAdjustedScore() != null
                        ? rec.getBestAdjustedScore().setScale(1, RoundingMode.HALF_UP).toPlainString()
                        : "-";
        return "[종목추천] "
                + rec.getStockName()
                + " ("
                + rec.getStockCode()
                + ")\n"
                + "점수 "
                + score
                + " · 등급 "
                + (rec.getSignalGrade() != null ? rec.getSignalGrade() : "-")
                + "\n"
                + "전략: "
                + (rec.getStrategyCodes() != null ? rec.getStrategyCodes() : "-")
                + "\n"
                + (rec.getReasonSummary() != null ? rec.getReasonSummary() : "")
                + "\n"
                + "승인: 앱 → 종목 추천 메뉴";
    }
}
