package com.noono0.stock.execution.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.execution.ExecutionPhase;
import com.noono0.stock.execution.config.ExecutionPhaseProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionRuntimeService {

    private final PlatformRuntimeConfigRepository platformRuntimeConfigRepository;
    private final ObjectMapper objectMapper;
    private final ExecutionPhaseProperties bootstrapProperties;

    private volatile ExecutionRuntimeConfig cachedConfig = new ExecutionRuntimeConfig();

    @PostConstruct
    void init() {
        platformRuntimeConfigRepository
                .findById(ExecutionRuntimeConfig.KEY)
                .ifPresentOrElse(
                        row -> cachedConfig = parseConfigJson(row.getConfigJson()),
                        () -> {
                            ExecutionRuntimeConfig defaults = buildDefaultsFromBootstrap();
                            persistConfig(defaults);
                            log.info(
                                    "【RUNTIME】 execution_runtime 기본값 DB 저장 phase={}",
                                    defaults.getPhase());
                        });
    }

    public ExecutionRuntimeConfig get() {
        return cachedConfig;
    }

    public ExecutionPhase currentPhase() {
        return ExecutionPhase.fromConfig(cachedConfig.getPhase());
    }

    @Transactional
    public ExecutionRuntimeConfig update(ExecutionRuntimeConfig incomingPatch) {
        ExecutionRuntimeConfig mergedConfig = mergeConfigs(cachedConfig, incomingPatch);
        persistConfig(mergedConfig);
        cachedConfig = mergedConfig;
        log.info("【RUNTIME】 실행 설정 변경 phase={}", mergedConfig.getPhase());
        return mergedConfig;
    }

    @Transactional
    public ExecutionRuntimeConfig setPhase(String phaseName) {
        ExecutionRuntimeConfig updatedConfig = deepCopy(cachedConfig);
        updatedConfig.setPhase(phaseName);
        return update(updatedConfig);
    }

    private ExecutionRuntimeConfig buildDefaultsFromBootstrap() {
        ExecutionRuntimeConfig defaults = new ExecutionRuntimeConfig();
        defaults.setPhase(bootstrapProperties.getPhase());
        defaults.getRecommendation().setMinAdjustedScore(bootstrapProperties.getBuyScoreThreshold());
        return defaults;
    }

    private void persistConfig(ExecutionRuntimeConfig config) {
        try {
            PlatformRuntimeConfigEntity entity = new PlatformRuntimeConfigEntity();
            entity.setConfigKey(ExecutionRuntimeConfig.KEY);
            entity.setConfigJson(objectMapper.writeValueAsString(config));
            platformRuntimeConfigRepository.save(entity);
        } catch (Exception exception) {
            throw new IllegalStateException("runtime config 저장 실패", exception);
        }
    }

    private ExecutionRuntimeConfig parseConfigJson(String json) {
        try {
            return objectMapper.readValue(json, ExecutionRuntimeConfig.class);
        } catch (Exception exception) {
            log.warn("【RUNTIME】 JSON 파싱 실패 — 기본값 사용: {}", exception.getMessage());
            return buildDefaultsFromBootstrap();
        }
    }

    private ExecutionRuntimeConfig mergeConfigs(
            ExecutionRuntimeConfig currentConfig, ExecutionRuntimeConfig incomingPatch) {
        ExecutionRuntimeConfig merged = deepCopy(currentConfig);
        if (StringUtils.hasText(incomingPatch.getPhase())) {
            merged.setPhase(incomingPatch.getPhase().trim().toUpperCase());
        }
        if (incomingPatch.getOrderQty() > 0) {
            merged.setOrderQty(incomingPatch.getOrderQty());
        }
        merged.setAutoOrderScanEnabled(incomingPatch.isAutoOrderScanEnabled());
        if (incomingPatch.getAutoOrderScanIntervalMs() > 0) {
            merged.setAutoOrderScanIntervalMs(incomingPatch.getAutoOrderScanIntervalMs());
        }
        merged.setMarketHoursOnly(incomingPatch.isMarketHoursOnly());
        merged.setRealTradingEnabled(incomingPatch.isRealTradingEnabled());
        if (incomingPatch.getRecommendation() != null) {
            mergeRecommendationRules(merged.getRecommendation(), incomingPatch.getRecommendation());
        }
        if (incomingPatch.getNotification() != null) {
            mergeNotificationChannels(merged.getNotification(), incomingPatch.getNotification());
        }
        return merged;
    }

    private static void mergeRecommendationRules(
            ExecutionRuntimeConfig.RecommendationRules targetRules,
            ExecutionRuntimeConfig.RecommendationRules incomingRules) {
        if (StringUtils.hasText(incomingRules.getMinGrade())) {
            targetRules.setMinGrade(incomingRules.getMinGrade());
        }
        if (incomingRules.getMinAdjustedScore() > 0) {
            targetRules.setMinAdjustedScore(incomingRules.getMinAdjustedScore());
        }
        if (incomingRules.getMaxPerDay() > 0) {
            targetRules.setMaxPerDay(incomingRules.getMaxPerDay());
        }
        targetRules.setDedupeByStock(incomingRules.isDedupeByStock());
        targetRules.setNotifyOnNew(incomingRules.isNotifyOnNew());
    }

    private static void mergeNotificationChannels(
            ExecutionRuntimeConfig.NotificationChannels targetChannels,
            ExecutionRuntimeConfig.NotificationChannels incomingChannels) {
        targetChannels.setTelegram(incomingChannels.isTelegram());
        targetChannels.setDiscord(incomingChannels.isDiscord());
        targetChannels.setEmail(incomingChannels.isEmail());
        targetChannels.setKakao(incomingChannels.isKakao());
        if (incomingChannels.getKakaoWebhookUrl() != null) {
            targetChannels.setKakaoWebhookUrl(incomingChannels.getKakaoWebhookUrl());
        }
        targetChannels.setSms(incomingChannels.isSms());
        if (incomingChannels.getSmsWebhookUrl() != null) {
            targetChannels.setSmsWebhookUrl(incomingChannels.getSmsWebhookUrl());
        }
        if (incomingChannels.getEmailTo() != null) {
            targetChannels.setEmailTo(incomingChannels.getEmailTo());
        }
    }

    private ExecutionRuntimeConfig deepCopy(ExecutionRuntimeConfig source) {
        try {
            return objectMapper.readValue(
                    objectMapper.writeValueAsString(source), ExecutionRuntimeConfig.class);
        } catch (Exception exception) {
            ExecutionRuntimeConfig fallback = new ExecutionRuntimeConfig();
            fallback.setPhase(source.getPhase());
            fallback.setOrderQty(source.getOrderQty());
            fallback.setAutoOrderScanEnabled(source.isAutoOrderScanEnabled());
            fallback.setAutoOrderScanIntervalMs(source.getAutoOrderScanIntervalMs());
            fallback.setMarketHoursOnly(source.isMarketHoursOnly());
            fallback.setRealTradingEnabled(source.isRealTradingEnabled());
            return fallback;
        }
    }
}
