package com.noono0.stock.risk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.notification.NotificationEvent;
import com.noono0.stock.notification.NotificationService;
import com.noono0.stock.risk.domain.RiskEvent;
import com.noono0.stock.risk.domain.enums.RiskEventType;
import com.noono0.stock.risk.repository.RiskEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEventService {

    private final RiskEventRepository eventRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public RiskEvent record(
            RiskEventType type, String severity, String message, String stockCode, String strategyType, Map<String, Object> detail) {
        RiskEvent e = new RiskEvent();
        e.setEventType(type.name());
        e.setSeverity(severity);
        e.setMessage(message);
        e.setStockCode(stockCode);
        e.setStrategyType(strategyType);
        if (detail != null) {
            try {
                e.setDetailJson(objectMapper.writeValueAsString(detail));
            } catch (Exception ex) {
                e.setDetailJson("{}");
            }
        }
        RiskEvent saved = eventRepository.save(e);
        log.warn("[RISK-EVENT] {} — {}", type, message);
        notificationService.publish(NotificationEvent.fromRisk(type, message, stockCode, strategyType));
        return saved;
    }
}
