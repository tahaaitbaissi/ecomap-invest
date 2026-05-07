package com.example.backend.controllers;

import com.example.backend.audit.Audited;
import com.example.backend.controllers.dto.AdminUserListItemResponse;
import com.example.backend.controllers.dto.AdminUserListResponse;
import com.example.backend.controllers.dto.AdminUserRoleUpdateRequest;
import com.example.backend.entities.Role;
import com.example.backend.entities.User;
import com.example.backend.repositories.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Users")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> list(
            @RequestParam(value = "emailLike", required = false) String emailLike,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size) {
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        var pr = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        var p = (emailLike != null && !emailLike.isBlank())
                ? userRepository.findByEmailContainingIgnoreCase(emailLike.trim(), pr)
                : userRepository.findAll(pr);
        var items = p.getContent().stream().map(AdminUserController::toListItem).toList();
        return ResponseEntity.ok(new AdminUserListResponse(items, p.getTotalElements(), safePage, safeSize));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "ADMIN_USER_ROLE_SET")
    public ResponseEntity<?> setRole(@PathVariable("id") UUID id, @Valid @RequestBody AdminUserRoleUpdateRequest req) {
        try {
            Role role;
            try {
                role = Role.valueOf(req.role().trim());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("role must be ROLE_INVESTOR or ROLE_ADMIN");
            }
            User u = userRepository.findById(id).orElseThrow();
            u.setRole(role.name());
            userRepository.save(u);
            return ResponseEntity.ok(toListItem(u));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    private static AdminUserListItemResponse toListItem(User u) {
        return new AdminUserListItemResponse(
                u.getId(),
                u.getEmail(),
                u.getCompanyName(),
                u.getRole(),
                u.getCreatedAt() != null ? u.getCreatedAt().toInstant().toString() : null);
    }
}

