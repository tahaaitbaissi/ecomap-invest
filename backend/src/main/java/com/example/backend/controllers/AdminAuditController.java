package com.example.backend.controllers;

import com.example.backend.audit.AuditLog;
import com.example.backend.audit.AuditLogRepository;
import com.example.backend.controllers.dto.AdminAuditLogItemResponse;
import com.example.backend.controllers.dto.AdminAuditLogListResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Audit")
@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminAuditLogListResponse> list(
            @RequestParam(value = "userEmail", required = false) String userEmail,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "success", required = false) Boolean success,
            @RequestParam(value = "from", required = false) Instant from,
            @RequestParam(value = "to", required = false) Instant to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        var pr = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "occurredAt"));

        // Start from a non-null "match all" spec (avoids ambiguous where(null) overloads).
        Specification<AuditLog> spec = (root, q, cb) -> cb.conjunction();
        if (userEmail != null && !userEmail.isBlank()) {
            String needle = userEmail.trim().toLowerCase();
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("userEmail")), "%" + needle + "%"));
        }
        if (action != null && !action.isBlank()) {
            String needle = action.trim().toLowerCase();
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("action")), "%" + needle + "%"));
        }
        if (success != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("success"), success));
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to));
        }

        var p = auditLogRepository.findAll(spec, pr);
        var items = p.getContent().stream().map(AdminAuditController::toDto).toList();
        return ResponseEntity.ok(new AdminAuditLogListResponse(items, p.getTotalElements(), safePage, safeSize));
    }

    private static AdminAuditLogItemResponse toDto(AuditLog l) {
        return new AdminAuditLogItemResponse(
                l.getId(),
                l.getAuditId(),
                l.getUserId(),
                l.getOccurredAt() != null ? l.getOccurredAt().toString() : null,
                l.getUserEmail(),
                l.getAction(),
                l.getMethod(),
                l.getArgsSummary(),
                l.getDurationMs(),
                l.getSuccess(),
                l.getErrorClass(),
                l.getErrorMessage());
    }
}

