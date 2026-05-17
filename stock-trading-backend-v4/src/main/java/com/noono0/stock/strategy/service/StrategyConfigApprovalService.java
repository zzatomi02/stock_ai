package com.noono0.stock.strategy.service;

import com.noono0.stock.strategy.domain.StrategyConfigChangeRequest;
import com.noono0.stock.strategy.domain.StrategyRuntimeSetting;
import com.noono0.stock.strategy.repository.StrategyConfigChangeRequestRepository;
import com.noono0.stock.strategy.repository.StrategyRuntimeSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StrategyConfigApprovalService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StrategyConfigChangeRequestRepository requestRepository;
    private final StrategyRuntimeSettingRepository runtimeRepository;

    @Transactional
    public StrategyConfigChangeRequest submit(
            String strategyType,
            String changeType,
            String fieldName,
            String oldValue,
            String newValue,
            String requestedBy) {
        StrategyConfigChangeRequest req = new StrategyConfigChangeRequest();
        req.setStrategyType(strategyType);
        req.setChangeType(changeType);
        req.setFieldName(fieldName);
        req.setOldValue(oldValue);
        req.setNewValue(newValue);
        req.setRequestedBy(requestedBy);
        req.setStatus("PENDING");
        return requestRepository.save(req);
    }

    @Transactional(readOnly = true)
    public List<StrategyConfigChangeRequest> listPending() {
        return requestRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    @Transactional
    public StrategyConfigChangeRequest approve(long id, String approvedBy) {
        StrategyConfigChangeRequest req =
                requestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("요청 없음"));
        if (!"PENDING".equals(req.getStatus())) {
            throw new IllegalStateException("이미 처리됨: " + req.getStatus());
        }
        req.setApprovedBy(approvedBy);
        req.setApprovedAt(LocalDateTime.now(KST));
        applyChange(req);
        req.setStatus("APPROVED");
        return requestRepository.save(req);
    }

    @Transactional
    public StrategyConfigChangeRequest reject(long id, String approvedBy, String reason) {
        StrategyConfigChangeRequest req =
                requestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("요청 없음"));
        req.setStatus("REJECTED");
        req.setApprovedBy(approvedBy);
        req.setApprovedAt(LocalDateTime.now(KST));
        req.setRejectReason(reason);
        return requestRepository.save(req);
    }

    private void applyChange(StrategyConfigChangeRequest req) {
        if ("RUNTIME_ENABLED".equals(req.getChangeType())) {
            StrategyRuntimeSetting runtime =
                    runtimeRepository
                            .findByTradeDateAndStrategyType(LocalDate.now(KST), req.getStrategyType())
                            .orElseGet(
                                    () -> {
                                        StrategyRuntimeSetting n = new StrategyRuntimeSetting();
                                        n.setTradeDate(LocalDate.now(KST));
                                        n.setStrategyType(req.getStrategyType());
                                        return n;
                                    });
            runtime.setEnabled(Boolean.parseBoolean(req.getNewValue()));
            runtime.setReason("승인 반영 #" + req.getId());
            runtime.setUpdatedBy(req.getApprovedBy());
            runtimeRepository.save(runtime);
        }
    }
}
