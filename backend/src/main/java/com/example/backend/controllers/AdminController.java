package com.example.backend.controllers;

import com.example.backend.services.CsvIngestionService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CsvIngestionService csvIngestionService;

    @PostMapping("/ingest-csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> ingestCsv() {
        int count = csvIngestionService.ingestFromCsv("data/mock_poi.csv");
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "rowsInserted", count
        ));
    }
}
