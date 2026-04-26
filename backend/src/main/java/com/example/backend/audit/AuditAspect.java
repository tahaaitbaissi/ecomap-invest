package com.example.backend.audit;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final int MAX_ARGS_CHARS = 4096;

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        long startNanos = System.nanoTime();
        String userEmail = currentUserEmailOrNull();

        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String method = sig.getDeclaringTypeName() + "." + sig.getName();
        String action = audited.action() == null ? "" : audited.action();

        String argsSummary = summarizeArgs(sig, pjp.getArgs(), audited);

        AuditLog row = null;
        if (audited.persist()) {
            row = new AuditLog();
            row.setOccurredAt(Instant.now());
            row.setUserEmail(userEmail);
            row.setAction(action);
            row.setMethod(method);
            row.setArgsSummary(argsSummary);
        }

        try {
            Object out = pjp.proceed();
            long durMs = (System.nanoTime() - startNanos) / 1_000_000L;
            log.info("AUDIT action={} user={} method={} durationMs={}", action, userEmail, method, durMs);
            if (row != null) {
                row.setDurationMs(durMs);
                row.setSuccess(true);
                auditLogRepository.save(row);
            }
            return out;
        } catch (Throwable t) {
            long durMs = (System.nanoTime() - startNanos) / 1_000_000L;
            log.warn("AUDIT_FAIL action={} user={} method={} durationMs={} err={}",
                    action, userEmail, method, durMs, t.getClass().getSimpleName());
            if (row != null) {
                row.setDurationMs(durMs);
                row.setSuccess(false);
                row.setErrorClass(t.getClass().getName());
                row.setErrorMessage(truncate(safeMessage(t), 2000));
                auditLogRepository.save(row);
            }
            throw t;
        }
    }

    private static String currentUserEmailOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) {
            return null;
        }
        String name = a.getName();
        return (name == null || name.isBlank()) ? null : name;
    }

    private static String summarizeArgs(MethodSignature sig, Object[] args, Audited audited) {
        String[] names = sig.getParameterNames();
        Set<String> masked = new HashSet<>();
        Arrays.stream(audited.maskParams()).filter(s -> s != null && !s.isBlank())
                .forEach(s -> masked.add(s.trim().toLowerCase()));

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            String n = (names != null && i < names.length) ? names[i] : ("arg" + i);
            sb.append(n).append("=");
            if (masked.contains(n.toLowerCase())) {
                sb.append("\"***\"");
            } else {
                sb.append(renderValue(args[i]));
            }
            if (sb.length() > MAX_ARGS_CHARS) {
                sb.append("…");
                break;
            }
        }
        sb.append("]");
        return truncate(sb.toString(), MAX_ARGS_CHARS);
    }

    private static String renderValue(Object v) {
        if (v == null) return "null";
        if (v instanceof String s) return "\"" + truncate(s, 256) + "\"";
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        if (v.getClass().isEnum()) return String.valueOf(v);
        if (v instanceof java.util.UUID) return "\"" + v + "\"";

        // Records: summarize primitive-ish components only
        if (v.getClass().isRecord()) {
            StringBuilder sb = new StringBuilder();
            sb.append(v.getClass().getSimpleName()).append("{");
            RecordComponent[] comps = v.getClass().getRecordComponents();
            for (int i = 0; i < comps.length; i++) {
                if (i > 0) sb.append(", ");
                RecordComponent c = comps[i];
                sb.append(c.getName()).append("=");
                try {
                    Object val = c.getAccessor().invoke(v);
                    sb.append(renderValue(val));
                } catch (Exception e) {
                    sb.append("<?>");
                }
                if (sb.length() > 1024) {
                    sb.append("…");
                    break;
                }
            }
            sb.append("}");
            return truncate(sb.toString(), 1024);
        }

        return "\"" + v.getClass().getSimpleName() + "\"";
    }

    private static String safeMessage(Throwable t) {
        String m = t.getMessage();
        return m == null ? "" : m;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }
}

