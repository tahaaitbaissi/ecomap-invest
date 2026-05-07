package com.example.backend.controllers;

import com.example.backend.audit.Audited;
import com.example.backend.controllers.dto.AdminPageResponse;
import com.example.backend.controllers.dto.AdminPoiSearchResponse;
import com.example.backend.controllers.dto.AdminPoiUpsertRequest;
import com.example.backend.services.admin.AdminPoiService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin POI")
@RestController
@RequestMapping("/api/v1/admin/poi")
@RequiredArgsConstructor
public class AdminPoiController {

    private final AdminPoiService adminPoiService;

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> search(
            @RequestParam(value = "minX", required = false) Double minX,
            @RequestParam(value = "minY", required = false) Double minY,
            @RequestParam(value = "maxX", required = false) Double maxX,
            @RequestParam(value = "maxY", required = false) Double maxY,
            @RequestParam(value = "typeTag", required = false) String typeTag,
            @RequestParam(value = "nameLike", required = false) String nameLike,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @RequestParam(value = "sort", defaultValue = "importedAt,desc") String sort) {
        try {
            int safePage = Math.max(page, 0);
            int safeSize = Math.min(Math.max(size, 1), 200);
            Sort s;
            try {
                String[] parts = sort.split(",");
                String field = parts[0].trim();
                String dir = parts.length >= 2 ? parts[1].trim() : "asc";
                Sort.Direction d = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
                // whitelist fields
                if (!field.equals("importedAt")
                        && !field.equals("name")
                        && !field.equals("typeTag")
                        && !field.equals("osmId")) {
                    field = "importedAt";
                }
                s = Sort.by(d, field);
            } catch (Exception e) {
                s = Sort.by(Sort.Direction.DESC, "importedAt");
            }
            var pr = PageRequest.of(safePage, safeSize, s);
            var p = adminPoiService.searchPage(minX, minY, maxX, maxY, typeTag, nameLike, pr);
            return ResponseEntity.ok(new AdminPageResponse<>(p.getContent(), p.getTotalElements(), safePage, safeSize));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "ADMIN_POI_CREATE")
    public ResponseEntity<?> create(@Valid @RequestBody AdminPoiUpsertRequest request) {
        try {
            return ResponseEntity.ok(adminPoiService.create(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "ADMIN_POI_UPDATE")
    public ResponseEntity<?> update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminPoiUpsertRequest request) {
        try {
            return ResponseEntity.ok(adminPoiService.update(id, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "ADMIN_POI_DELETE")
    public ResponseEntity<?> delete(@PathVariable("id") UUID id) {
        try {
            adminPoiService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }
}

