package com.scuec.icpc.common.log;

import com.scuec.icpc.common.security.SecurityUtils;
import com.scuec.icpc.common.security.UserPrincipal;
import com.scuec.icpc.module.log.entity.OperationLog;
import com.scuec.icpc.module.log.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 关键操作审计切面。审计失败不能影响业务主流程。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogMapper operationLogMapper;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        int status = 200;
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            status = 500;
            throw throwable;
        } finally {
            try {
                saveLog(auditLog.value(), status, System.currentTimeMillis() - start);
            } catch (Exception ex) {
                log.warn("写入操作日志失败：{}", ex.getMessage());
            }
        }
    }

    private void saveLog(String action, int status, long duration) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        OperationLog logEntity = new OperationLog();
        logEntity.setAction(action);
        logEntity.setMethod(request.getMethod());
        logEntity.setPath(request.getRequestURI());
        logEntity.setIp(clientIp(request));
        logEntity.setStatus(status);
        logEntity.setDuration(duration);
        logEntity.setCreateTime(LocalDateTime.now());
        try {
            UserPrincipal principal = SecurityUtils.currentUser();
            logEntity.setMemberId(principal.getMemberId());
            logEntity.setUsername(principal.getName());
        } catch (Exception ignored) {
            logEntity.setUsername("anonymous");
        }
        operationLogMapper.insert(logEntity);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
