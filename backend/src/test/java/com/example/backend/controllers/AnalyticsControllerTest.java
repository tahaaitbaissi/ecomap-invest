package com.example.backend.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.ZoneStatsResponse;
import com.example.backend.services.AnalyticsService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController analyticsController;

    private static UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(
                "a@a.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void zone_ok() {
        UUID pid = UUID.randomUUID();
        ZoneStatsResponse out = new ZoneStatsResponse(
                "8928308280fffff",
                pid,
                1000.0,
                123,
                Map.of("amenity=school", 1),
                Map.of("amenity=cafe", 2),
                List.of());

        when(analyticsService.getZoneStatistics(eq("8928308280fffff"), eq(pid), eq("a@a.com")))
                .thenReturn(out);

        ResponseEntity<?> res = analyticsController.zone(auth(), "8928308280fffff", pid);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }
}

