package com.noono0.stock.ops.audit;

import com.noono0.stock.ops.audit.domain.AuditLog;
import com.noono0.stock.ops.audit.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditLogMapper auditLogMapper;

    /**
     * ops 전체를 넣으면 {@link AuditLogMapper#insert} 까지 매칭되어 감사 저장 → 감사 저장… 무한 재귀(StackOverflowError).
     * ops.audit.* 는 제외한다.
     */
    private static final String PC_BASE =
            "(execution(* com.noono0.stock.broker..*.*(..)) "
                    + "|| (execution(* com.noono0.stock.ops..*.*(..)) && !execution(* com.noono0.stock.ops.audit..*.*(..))) "
                    + "|| execution(* com.noono0.stock.strategy..*.*(..)) "
                    + "|| execution(* com.noono0.stock.news..*.*(..)))";

    @AfterReturning(pointcut = PC_BASE)
    public void ok(JoinPoint jp) {
        save(jp, true, "ok");
    }

    @AfterThrowing(pointcut = PC_BASE, throwing = "ex")
    public void err(JoinPoint jp, Exception ex) {
        save(
                jp,
                false,
                ex.getClass().getSimpleName() + ": " + (ex.getMessage() != null ? ex.getMessage() : ""));
    }

    private void save(JoinPoint jp, boolean success, String detail) {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            String path = "";
            if (attrs instanceof ServletRequestAttributes s && s.getRequest() != null) {
                path = s.getRequest().getMethod() + " " + s.getRequest().getRequestURI();
            }
            AuditLog a = new AuditLog();
            a.setAction(jp.getSignature().toShortString() + " " + path);
            a.setDetail(detail);
            a.setSuccess(success);
            a.setCreatedAt(LocalDateTime.now());
            auditLogMapper.insert(a);
        } catch (Exception exception) {
            log.debug("audit log failed: {}", exception.getMessage());
        }
    }
}
