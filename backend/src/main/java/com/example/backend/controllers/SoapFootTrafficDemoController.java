package com.example.backend.controllers;

import com.ecomap.foottraffic.simulation.FootTrafficSimulationEngine;
import com.ecomap.ftsim.ws.xml.SimulateCellRequest;
import com.ecomap.ftsim.ws.xml.TagCountRow;
import com.example.backend.foottraffic.config.FootTrafficProperties;
import com.example.backend.foottraffic.repositories.FootTrafficZoneParamsRepository;
import com.example.backend.foottraffic.services.FootTrafficSoapSimulationClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo: backend → SOAP foot-traffic simulator (same contract as batch recompute). Public for lab /
 * Postman (no JWT).
 */
@Tag(name = "SOAP foot-traffic demo", description = "Distributed foot-traffic simulation via SOAP")
@RestController
@RequestMapping("/api/v1/soap-ft")
@RequiredArgsConstructor
public class SoapFootTrafficDemoController {

    private static final int SCENARIO_DEFAULT = 0;

    private final FootTrafficSoapSimulationClient soapClient;
    private final FootTrafficZoneParamsRepository zoneParamsRepository;
    private final FootTrafficProperties footTrafficProperties;

    public record TagRow(String typeTag, long count) {}

    public record SoapFtDemoRequest(
            String h3Index,
            List<TagRow> tagRows,
            double populationDensity,
            double avgIncome) {}

    @Operation(summary = "Run one cell through remote SOAP foot-traffic simulation")
    @PostMapping("/simulate")
    public ResponseEntity<?> simulate(@RequestBody SoapFtDemoRequest body) {
        if (!soapClient.isReady()) {
            return ResponseEntity.status(503).body("SOAP foot-traffic client disabled or misconfigured");
        }
        SimulateCellRequest rq = new SimulateCellRequest();
        rq.setH3Index(body.h3Index());
        rq.setScenarioId(SCENARIO_DEFAULT);
        rq.setJitterSalt(footTrafficProperties.getJitterSalt());
        rq.setPopulationDensity(body.populationDensity());
        rq.setAvgIncome(body.avgIncome());
        if (body.tagRows() != null) {
            for (TagRow tr : body.tagRows()) {
                if (tr == null || tr.typeTag() == null) {
                    continue;
                }
                TagCountRow row = new TagCountRow();
                row.setTypeTag(tr.typeTag());
                row.setCount(tr.count());
                rq.getTagRow().add(row);
            }
        }
        var remote = soapClient.simulateCellAsync(rq).join();
        if (remote != null) {
            return ResponseEntity.ok(remote);
        }
        var local =
                FootTrafficSimulationEngine.simulate(
                        com.ecomap.foottraffic.simulation.PoiFootTrafficTagAggregator.aggregate(
                                body.tagRows() == null
                                        ? List.of()
                                        : body.tagRows().stream()
                                                .map(tr -> new Object[] {tr.typeTag(), tr.count()})
                                                .toList()),
                        body.populationDensity(),
                        body.avgIncome(),
                        body.h3Index(),
                        footTrafficProperties.getJitterSalt(),
                        arch ->
                                zoneParamsRepository
                                        .findById(arch)
                                        .orElseGet(
                                                () ->
                                                        zoneParamsRepository
                                                                .findById("RESIDENTIAL")
                                                                .orElseThrow())
                                        .toSnapshot());
        return ResponseEntity.ok(local);
    }
}
