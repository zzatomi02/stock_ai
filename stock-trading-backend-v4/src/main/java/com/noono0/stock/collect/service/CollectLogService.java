package com.noono0.stock.collect.service;

import com.noono0.stock.collect.domain.CollectErrorLog;
import com.noono0.stock.collect.domain.CollectJobLog;
import com.noono0.stock.collect.enums.CollectJobStatus;
import com.noono0.stock.collect.repository.CollectErrorLogRepository;
import com.noono0.stock.collect.repository.CollectJobLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CollectLogService {
    private final CollectJobLogRepository jobLogRepository;
    private final CollectErrorLogRepository errorLogRepository;

    @Transactional
    public CollectJobLog startJob(String jobName, String provider, String targetType, String targetValue) {
        CollectJobLog log = new CollectJobLog();
        log.setJobName(jobName);
        log.setProvider(provider);
        log.setTargetType(targetType);
        log.setTargetValue(targetValue);
        log.setStartedAt(LocalDateTime.now());
        log.setStatus(CollectJobStatus.RUNNING.name());
        return jobLogRepository.save(log);
    }

    @Transactional
    public void finishJob(
            CollectJobLog log,
            int requestCount,
            int responseCount,
            int savedCount,
            int duplicateCount,
            int skippedCount,
            int errorCount,
            CollectJobStatus status,
            String message) {
        log.setEndedAt(LocalDateTime.now());
        log.setRequestCount(requestCount);
        log.setResponseCount(responseCount);
        log.setSavedCount(savedCount);
        log.setDuplicateCount(duplicateCount);
        log.setSkippedCount(skippedCount);
        log.setErrorCount(errorCount);
        log.setStatus(status.name());
        log.setMessage(message);
        jobLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(
            String jobName,
            String provider,
            String targetType,
            String targetValue,
            String errorType,
            String errorMessage,
            Throwable throwable,
            String requestUrl,
            String responseBody) {
        CollectErrorLog row = new CollectErrorLog();
        row.setJobName(jobName);
        row.setProvider(provider);
        row.setTargetType(targetType);
        row.setTargetValue(targetValue);
        row.setErrorType(errorType);
        row.setErrorMessage(errorMessage);
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            row.setStackTrace(sw.toString());
        }
        row.setRequestUrl(requestUrl);
        row.setResponseBody(truncate(responseBody, 8000));
        errorLogRepository.save(row);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
