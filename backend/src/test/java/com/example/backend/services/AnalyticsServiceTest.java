package com.example.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.backend.entities.Demographics;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.PoiRepository;
import com.example.backend.services.profile.DynamicProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private PoiRepository poiRepository;
    @Mock
    private DemographicsRepository demographicsRepository;
    @Mock
    private DynamicProfileService dynamicProfileService;

    @Test
    void rejectsWhenProfileNotOwned() {
        AnalyticsService svc = new AnalyticsService(poiRepository, demographicsRepository, dynamicProfileService);

        UUID pid = UUID.randomUUID();
        when(dynamicProfileService.getOwnedActiveEntity("a@a.com", pid))
                .thenThrow(new AccessDeniedException("You do not own this profile"));

        assertThrows(
                AccessDeniedException.class,
                () -> svc.getZoneStatistics("8928308280fffff", pid, "a@a.com"));
    }

    @Test
    void computesEstimatedFootTraffic_andCounts() throws Exception {
        AnalyticsService svc = new AnalyticsService(poiRepository, demographicsRepository, dynamicProfileService);

        UUID pid = UUID.randomUUID();
        DynamicProfile p = new DynamicProfile();
        p.setId(pid);
        p.setUserId(UUID.randomUUID());
        p.setGeneratedAt(Instant.now());
        ObjectMapper om = new ObjectMapper();
        JsonNode drivers = om.readTree("[{\"tag\":\"amenity=school\",\"weight\":1.0}]");
        JsonNode competitors = om.readTree("[{\"tag\":\"amenity=cafe\",\"weight\":1.0}]");
        p.setDriversConfig(drivers);
        p.setCompetitorsConfig(competitors);
        when(dynamicProfileService.getOwnedActiveEntity("a@a.com", pid)).thenReturn(p);

        when(poiRepository.countByTypeTagWithinHex(eq("amenity=school"), any())).thenReturn(3L);
        when(poiRepository.countByTypeTagWithinHex(eq("amenity=cafe"), any())).thenReturn(2L);

        Demographics d = new Demographics();
        d.setH3Index("h");
        d.setPopulationDensity(10_000.0);
        when(demographicsRepository.findById(any())).thenReturn(Optional.of(d));

        when(poiRepository.findTopPoisWithinHex(any(), anyInt())).thenReturn(List.of());

        var out = svc.getZoneStatistics("8928308280fffff", pid, "a@a.com");
        assertNotNull(out);
        // estimated = min(50000, round(P*0.4 + D*200)) = 10000*0.4 + 3*200 = 4000 + 600 = 4600
        assertEquals(4600, out.estimatedFootTraffic());
        assertEquals(3, out.driverCounts().get("amenity=school"));
        assertEquals(2, out.competitorCounts().get("amenity=cafe"));
    }
}

