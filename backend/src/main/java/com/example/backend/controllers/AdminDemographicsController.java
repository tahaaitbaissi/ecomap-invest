package com.example.backend.controllers;

import com.example.backend.audit.Audited;
import com.example.backend.controllers.dto.AdminDemographicsUpsertRequest;
import com.example.backend.services.admin.AdminDemographicsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Demographics")
@RestController
@RequestMapping("/api/v1/admin/demographics")
@RequiredArgsConstructor
public class AdminDemographicsController {

    private final AdminDemographicsService adminDemographicsService;

    @GetMapping("/{h3Index}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> get(@PathVariable("h3Index") String h3Index) {
        try {
            return ResponseEntity.ok(adminDemographicsService.get(h3Index));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    @PutMapping("/{h3Index}")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "ADMIN_DEMOGRAPHICS_UPSERT")
    public ResponseEntity<?> upsert(
            @PathVariable("h3Index") String h3Index,
            @Valid @RequestBody AdminDemographicsUpsertRequest request) {
        try {
            return ResponseEntity.ok(adminDemographicsService.upsert(h3Index, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @DeleteMapping("/{h3Index}")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "ADMIN_DEMOGRAPHICS_DELETE")
    public ResponseEntity<?> delete(@PathVariable("h3Index") String h3Index) {
        adminDemographicsService.delete(h3Index);
        return ResponseEntity.noContent().build();
    }
}

