package com.example.backend.services.explain;

import com.example.backend.controllers.dto.HexExplanationContextDto;
import com.example.backend.entities.Demographics;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.DynamicProfileRepository;
import com.example.backend.repositories.H3HexagonRepository;
import com.example.backend.scoring.HexScoringConfig;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.example.backend.scoring.HexagonRawScoringSupport.HexRawParts;
import com.example.backend.scoring.HexagonRawScoringSupport.TagHexContribution;
import com.example.backend.scoring.RawScoreRefBounds;
import com.example.backend.services.ProfileScoreScaleService;
import com.uber.h3core.H3Core;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Builds a {@link HexExplanationContextDto}: grid-backed aggregation for parent H3 cells, per-tag
 * counts summed across disjoint child hexes, and average raw aligned with heatmap rollup semantics.
 */
@Service
@RequiredArgsConstructor
public class HexExplanationContextBuilder {

    private final H3Core h3;
    private final H3HexagonRepository h3HexagonRepository;
    private final HexagonRawScoringSupport rawScoringSupport;
    private final ProfileScoreScaleService profileScoreScaleService;
    private final DemographicsRepository demographicsRepository;
    private final DynamicProfileRepository dynamicProfileRepository;

    @Value("${app.hexagon.resolution:9}")
    private int gridResolution;

    @Value("${app.hexagon.demographic-density-cap:20000.0}")
    private double demographicDensityCap;

    /**
     * @param viewportCellCount if non-null and &gt;=1, compute {@link HexExplanationContextDto#computedDisplayScore} like the map
     *     overlay (same formula as {@link ProfileScoreScaleService#toHeatmapDisplayScore}).
     */
    public HexExplanationContextDto build(
            UUID profileId, String h3Index, Integer viewportCellCount) {
        HexScoringConfig cfg = rawScoringSupport.buildConfigForProfile(profileId);
        DynamicProfile profile =
                dynamicProfileRepository.findById(profileId).orElseThrow();

        List<String> leavesOnGrid = expandToLeavesOnSeedGrid(h3Index);
        int inputRes = h3.getResolution(h3.stringToH3(h3Index));
        boolean aggregated = inputRes < gridResolution;

        if (leavesOnGrid.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cette cellule H3 ne correspond à aucune maille présente dans la grille modèle (résolution "
                            + gridResolution
                            + "). Zoomez ou cliquez sur une cellule qui intersecte la zone pilote.");
        }

        HexRawSummaries sum = summarizeLeaves(leavesOnGrid, cfg);

        RawScoreRefBounds ref = profileScoreScaleService.resolveRefBounds(profileId, cfg);
        int vc =
                viewportCellCount != null && viewportCellCount >= 1
                        ? viewportCellCount
                        : Math.max(leavesOnGrid.size(), 1);
        Double display = ProfileScoreScaleService.toHeatmapDisplayScore(sum.averageRaw(), ref, vc);

        var drivers =
                aggregateTagContributionsAcrossLeaves(leavesOnGrid, cfg, true).stream()
                        .map(d -> new HexExplanationContextDto.TagContributionRow(
                                d.tag(), d.weight(), d.countSum(), d.weightedSum()))
                        .toList();
        var comps =
                aggregateTagContributionsAcrossLeaves(leavesOnGrid, cfg, false).stream()
                        .map(d -> new HexExplanationContextDto.TagContributionRow(
                                d.tag(), d.weight(), d.countSum(), d.weightedSum()))
                        .toList();

        long totalUwComp = sum.uwCompSum();

        LatCenter center = centerOfCell(h3Index);

        DemoAgg demoAgg = demographicsAggregate(leavesOnGrid, cfg);

        String aggregationNote =
                aggregated
                        ? ("Cellule H3 résolution "
                                + inputRes
                                + " : détail agrégé sur "
                                + leavesOnGrid.size()
                                + " sous-hexagones présents dans la grille modèle à résolution "
                                + gridResolution
                                + ". Le score carte pour une vue agrégée est la normalisation globale "
                                + "du brut moyen des cellules enfants présentes dans l'échantillon affiché; "
                                + "ici le brut affiché côté explication utilise la même moyenne sur ce sous-ensemble de la grille.")
                        : ("Hexagone unique résolution " + gridResolution + " présent dans la grille modèle.");

        return new HexExplanationContextDto(
                profileId,
                profile.getName(),
                profile.getUserQuery(),
                h3Index,
                inputRes,
                aggregated,
                gridResolution,
                leavesOnGrid.size(),
                aggregationNote,
                center.lat,
                center.lng,
                sum.averageRaw(),
                ref.min(),
                ref.max(),
                ref.flat(),
                display,
                sum.averagePTerm(),
                sum.sumDrivers(),
                sum.sumCompetitors(),
                drivers,
                comps,
                new HexExplanationContextDto.DemographicsSnapshot(
                        cfg.useDemographics(), demographicDensityCap, sum.averagePTerm()),
                totalUwComp,
                demoAgg.avgDensity.orElse(null),
                demoAgg.avgIncome.orElse(null));
    }

    private List<String> expandToLeavesOnSeedGrid(String h3Index) {
        long cell = h3.stringToH3(h3Index);
        int r = h3.getResolution(cell);
        if (r > gridResolution) {
            throw new IllegalArgumentException(
                    "h3Index resolution " + r + " is finer than supported grid resolution " + gridResolution);
        }
        if (r == gridResolution) {
            if (!h3HexagonRepository.existsById(h3Index)) {
                return List.of();
            }
            return List.of(h3Index);
        }
        List<Long> children = h3.cellToChildren(cell, gridResolution);
        List<String> out = new ArrayList<>();
        for (Long idx : children) {
            String s = h3.h3ToString(idx);
            if (h3HexagonRepository.existsById(s)) {
                out.add(s);
            }
        }
        return out;
    }

    private record HexRawSummaries(
            double averageRaw, double averagePTerm, double sumDrivers, double sumCompetitors, long uwCompSum) {}

    private HexRawSummaries summarizeLeaves(List<String> leavesOnGrid, HexScoringConfig cfg) {
        if (leavesOnGrid.isEmpty()) {
            return new HexRawSummaries(0.0, 0.0, 0.0, 0.0, 0L);
        }
        double rawSum = 0;
        double pSum = 0;
        double dSum = 0;
        double cSum = 0;
        long uwTotal = 0;
        int n = 0;
        for (String leaf : leavesOnGrid) {
            HexRawParts p = rawScoringSupport.computeParts(leaf, cfg);
            double raw =
                    p.driversWeighted() + p.pTerm() - p.competitorsWeighted();
            rawSum += raw;
            pSum += p.pTerm();
            dSum += p.driversWeighted();
            cSum += p.competitorsWeighted();
            uwTotal += rawScoringSupport.competitorCountWithinHex(leaf, cfg);
            n++;
        }
        double inv = 1.0 / n;
        return new HexRawSummaries(rawSum * inv, pSum * inv, dSum, cSum, uwTotal);
    }

    /** Sum counts across leaves for identical tags (disjoint geometries). */
    private List<MergedTagRow> aggregateTagContributionsAcrossLeaves(
            List<String> leavesOnGrid, HexScoringConfig cfg, boolean drivers) {
        Map<String, MergedAccumulator> agg = new LinkedHashMap<>();
        List<TagHexContribution> template =
                drivers
                        ? cfg.driverTags().stream()
                                .map(t -> new TagHexContribution(t.tag(), t.weight(), 0, 0))
                                .toList()
                        : cfg.competitorTags().stream()
                                .map(t -> new TagHexContribution(t.tag(), t.weight(), 0, 0))
                                .toList();
        for (TagHexContribution t : template) {
            agg.put(t.tag(), new MergedAccumulator(0));
        }

        for (String leaf : leavesOnGrid) {
            List<TagHexContribution> rows =
                    drivers
                            ? rawScoringSupport.listDriverContributions(leaf, cfg)
                            : rawScoringSupport.listCompetitorContributions(leaf, cfg);
            for (TagHexContribution row : rows) {
                MergedAccumulator cell = agg.get(row.tag());
                if (cell == null) {
                    continue;
                }
                cell.countSum += row.countInside();
            }
        }

        List<MergedTagRow> out = new ArrayList<>();
        for (TagHexContribution t : template) {
            MergedAccumulator m = agg.get(t.tag());
            long cSum = m == null ? 0 : m.countSum;
            out.add(new MergedTagRow(t.tag(), t.weight(), cSum, cSum * t.weight()));
        }
        return out;
    }

    private static final class MergedAccumulator {
        private long countSum;

        private MergedAccumulator(long countSum) {
            this.countSum = countSum;
        }
    }

    private record MergedTagRow(String tag, double weight, long countSum, double weightedSum) {}

    private record DemoAgg(Optional<Double> avgDensity, Optional<Double> avgIncome) {}

    private DemoAgg demographicsAggregate(List<String> leavesOnGrid, HexScoringConfig cfg) {
        if (!cfg.useDemographics() || leavesOnGrid.isEmpty()) {
            return new DemoAgg(Optional.empty(), Optional.empty());
        }
        double dSum = 0;
        double iSum = 0;
        int dn = 0;
        int in = 0;
        for (String leaf : leavesOnGrid) {
            Optional<Demographics> d = demographicsRepository.findById(leaf);
            if (d.isEmpty()) {
                continue;
            }
            Demographics row = d.get();
            if (row.getPopulationDensity() != null) {
                dSum += row.getPopulationDensity();
                dn++;
            }
            if (row.getAvgIncome() != null) {
                iSum += row.getAvgIncome();
                in++;
            }
        }
        return new DemoAgg(
                dn > 0 ? Optional.of(dSum / dn) : Optional.empty(),
                in > 0 ? Optional.of(iSum / in) : Optional.empty());
    }

    private LatCenter centerOfCell(String h3Index) {
        long cell = h3.stringToH3(h3Index);
        com.uber.h3core.util.LatLng c = h3.cellToLatLng(cell);
        return new LatCenter(c.lat, c.lng);
    }

    private record LatCenter(double lat, double lng) {}
}
