package com.example.backend.services.opportunity;

import com.example.backend.entities.DynamicProfile;
import com.example.backend.foottraffic.config.FootTrafficProperties;
import com.example.backend.foottraffic.services.FootTrafficService;
import com.example.backend.scoring.HexScoringConfig;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.uber.h3core.H3Core;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Opening-a-business opportunity at a point: demand (drivers in the covering H3 cell and/or radius + demographics),
 * competition/saturation bands, YAML-driven fit bonuses.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpportunityScoreService {

    private final HexagonRawScoringSupport rawScoringSupport;
    private final FootTrafficService footTrafficService;
    private final FootTrafficProperties footTrafficProperties;
    private final BusinessFitCatalog businessFitCatalog;
    private final OpportunityExplanationService opportunityExplanationService;
    private final H3Core h3;

    @Value("${app.opportunity.demand-radius-meters:800}")
    private double demandRadiusMeters;

    @Value("${app.opportunity.competitor-radius-meters:500}")
    private double competitorRadiusMeters;

    @Value("${app.opportunity.fit-radius-meters:600}")
    private double fitRadiusMeters;

    @Value("${app.opportunity.demand-scale:8.0}")
    private double demandScale;

    @Value("${app.hexagon.resolution:9}")
    private int h3Resolution;

    public com.example.backend.controllers.dto.OpportunitySimulateResponse compute(
            DynamicProfile profile, double lat, double lng, HexScoringConfig cfg, boolean explain) {
        String h3Index = cellString(lat, lng);

        BusinessFitCatalog.ResolvedArchetype arch =
                businessFitCatalog.matchArchetype(profile.getName(), profile.getUserQuery());

        HexagonRawScoringSupport.HexRawParts hexParts = rawScoringSupport.computeParts(h3Index, cfg);
        double weightedDriversNearby =
                rawScoringSupport.weightedDriversNearLatLng(lat, lng, demandRadiusMeters, cfg);
        double pTerm = hexParts.pTerm();
        double pNorm = cfg.useDemographics() && pTerm > 0 ? pTerm / 0.3 : 0.0;
        double trafficNorm = footTrafficService.getPeakHourlyNorm(h3Index).orElse(0.0);
        double demandNorm =
                trafficNorm > 0
                        ? pNorm * footTrafficProperties.getBlendAlpha()
                                + trafficNorm * (1.0 - footTrafficProperties.getBlendAlpha())
                        : pNorm;
        double popPoints =
                (cfg.useDemographics() || trafficNorm > 0)
                        ? Math.min(35.0, demandNorm * 35.0)
                        : 0.0;
        // Demand uses max(hex cell, radius) drivers so clicks stay consistent with heatmap cell totals.
        double driverPointsHex = Math.min(70.0, Math.log1p(hexParts.driversWeighted()) * demandScale);
        double driverPointsNearby = Math.min(70.0, Math.log1p(weightedDriversNearby) * demandScale);
        double driverPoints = Math.max(driverPointsHex, driverPointsNearby);
        double demandScore = Math.min(100.0, driverPoints + popPoints);

        long compNearby = rawScoringSupport.competitorCountNearLatLng(lat, lng, competitorRadiusMeters, cfg);
        long compInHex = rawScoringSupport.competitorCountWithinHex(h3Index, cfg);
        long compForBands = Math.max(compNearby, compInHex);
        double competitionPenalty;
        double clusterEffectBonus;

        if (compForBands <= 0) {
            // No competition should not be penalized (counterintuitive inversion).
            competitionPenalty = 0.0;
            clusterEffectBonus = 0.0;
        } else if (compForBands <= 3) {
            // Mild validated-market effect, but never makes competition strictly better than zero.
            competitionPenalty = 2.0;
            clusterEffectBonus = 0.0;
        } else if (compForBands <= 6) {
            competitionPenalty = 12.0;
            clusterEffectBonus = 0;
        } else {
            competitionPenalty = 28.0;
            clusterEffectBonus = 0;
        }

        double fitBonus = computeFit(lat, lng, arch);

        double raw =
                demandScore - competitionPenalty + clusterEffectBonus + fitBonus;
        double opportunityScore = Math.max(0.0, Math.min(100.0, raw));

        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("weightedDriversInHex", hexParts.driversWeighted());
        metrics.put("weightedDriversDemandRadius", weightedDriversNearby);
        metrics.put("demographicsPTerm", pTerm);
        metrics.put("footTrafficNorm", trafficNorm);
        metrics.put("demandNorm", demandNorm);
        metrics.put("competitorRadiusMeters", competitorRadiusMeters);
        metrics.put("demandRadiusMeters", demandRadiusMeters);
        metrics.put("competitorCountInHex", (double) compInHex);
        metrics.put("competitorCountForBands", (double) compForBands);

        String explanation =
                explain
                        ? opportunityExplanationService
                                .explain(
                                        profile.getName(),
                                        opportunityScore,
                                        demandScore,
                                        competitionPenalty,
                                        clusterEffectBonus,
                                        fitBonus,
                                        compNearby,
                                        compInHex,
                                        arch.id())
                                .orElse("")
                        : "";

        return new com.example.backend.controllers.dto.OpportunitySimulateResponse(
                round2(opportunityScore),
                round2(demandScore),
                round2(competitionPenalty),
                round2(clusterEffectBonus),
                round2(fitBonus),
                h3Index,
                compNearby,
                compInHex,
                arch.id(),
                explanation,
                metrics);
    }

    private String cellString(double lat, double lng) {
        long cell = h3.latLngToCell(lat, lng, h3Resolution);
        return h3.h3ToString(cell);
    }

    private double computeFit(double lat, double lng, BusinessFitCatalog.ResolvedArchetype arch) {
        if (arch.preferredTags() == null || arch.preferredTags().isEmpty() || arch.maxBonus() <= 0) {
            return 0;
        }
        double bonus = 0;
        for (String tag : arch.preferredTags()) {
            long c = rawScoringSupport.countNearbyByTag(lat, lng, fitRadiusMeters, tag);
            bonus += c * arch.bonusPerHit();
            if (bonus >= arch.maxBonus()) {
                return arch.maxBonus();
            }
        }
        return Math.min(arch.maxBonus(), bonus);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
