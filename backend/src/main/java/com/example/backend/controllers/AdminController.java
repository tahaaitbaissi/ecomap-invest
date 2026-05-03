package com.example.backend.controllers;

import com.example.backend.services.CsvIngestionService;
import com.example.backend.services.EnterpriseXlsxIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final CsvIngestionService csvIngestionService;
    private final EnterpriseXlsxIngestionService enterpriseXlsxIngestionService;

    @Operation(summary = "Ingest mock POI CSV (admin)")
    @PostMapping("/ingest-csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> ingestCsv() {
        int count = csvIngestionService.ingestFromCsv("data/mock_poi.csv");
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "rowsInserted", count
        ));
    }

    @Operation(summary = "Ingest enterprise directory XLSX into poi (admin)")
    @PostMapping("/ingest-enterprises")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> ingestEnterprises() {
        int count = enterpriseXlsxIngestionService.ingestFromClasspath("data/enterprises.xlsx");
        return ResponseEntity.ok(
                Map.of("status", "success", "rowsInserted", count));
    }
}
