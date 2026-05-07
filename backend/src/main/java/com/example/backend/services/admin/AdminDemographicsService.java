package com.example.backend.services.admin;

import com.example.backend.controllers.dto.AdminDemographicsResponse;
import com.example.backend.controllers.dto.AdminDemographicsUpsertRequest;
import com.example.backend.entities.Demographics;
import com.example.backend.repositories.DemographicsRepository;
import java.sql.Timestamp;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDemographicsService {

    private final DemographicsRepository demographicsRepository;
    private final ScoreCacheVersionService scoreCacheVersionService;

    @Transactional(readOnly = true)
    public AdminDemographicsResponse get(String h3Index) {
        Demographics row = demographicsRepository.findById(h3Index).orElseThrow();
        return toDto(row);
    }

    @Transactional
    public AdminDemographicsResponse upsert(String h3Index, AdminDemographicsUpsertRequest req) {
        if (h3Index == null || h3Index.isBlank()) {
            throw new IllegalArgumentException("h3Index required");
        }
        if (req.populationDensity() != null && !Double.isFinite(req.populationDensity())) {
            throw new IllegalArgumentException("populationDensity must be finite");
        }
        if (req.avgIncome() != null && !Double.isFinite(req.avgIncome())) {
            throw new IllegalArgumentException("avgIncome must be finite");
        }
        Demographics row = demographicsRepository.findById(h3Index).orElseGet(Demographics::new);
        row.setH3Index(h3Index);
        row.setPopulationDensity(req.populationDensity());
        row.setAvgIncome(req.avgIncome());
        row.setLastUpdated(Timestamp.from(Instant.now()));
        AdminDemographicsResponse out = toDto(demographicsRepository.save(row));
        scoreCacheVersionService.bumpDemoVersion();
        return out;
    }

    @Transactional
    public void delete(String h3Index) {
        demographicsRepository.deleteById(h3Index);
        scoreCacheVersionService.bumpDemoVersion();
    }

    private static AdminDemographicsResponse toDto(Demographics row) {
        return new AdminDemographicsResponse(
                row.getH3Index(),
                row.getPopulationDensity(),
                row.getAvgIncome(),
                row.getLastUpdated() != null ? row.getLastUpdated().toInstant().toString() : null);
    }
}

