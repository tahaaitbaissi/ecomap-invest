package com.ecomap.soap.ft.ws;

import com.ecomap.foottraffic.simulation.FootTrafficSimulationEngine;
import com.ecomap.foottraffic.simulation.PoiFootTrafficTagAggregator;
import com.ecomap.foottraffic.simulation.PoiTagCounts;
import com.ecomap.ftsim.ws.xml.SimulateCellRequest;
import com.ecomap.ftsim.ws.xml.SimulateCellResponse;
import com.ecomap.ftsim.ws.xml.TagCountRow;
import com.ecomap.soap.ft.service.ZoneParamsRegistry;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class FootTrafficSimulationEndpoint {

    public static final String NAMESPACE = "http://ecomap.example.com/foottraffic/ws";

    private final ZoneParamsRegistry zoneParamsRegistry;

    public FootTrafficSimulationEndpoint(ZoneParamsRegistry zoneParamsRegistry) {
        this.zoneParamsRegistry = zoneParamsRegistry;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "simulateCellRequest")
    @ResponsePayload
    public SimulateCellResponse simulate(@RequestPayload SimulateCellRequest request) {
        List<Object[]> rows = new ArrayList<>();
        if (request.getTagRow() != null) {
            for (TagCountRow tr : request.getTagRow()) {
                if (tr == null || tr.getTypeTag() == null) {
                    continue;
                }
                rows.add(new Object[] {tr.getTypeTag(), tr.getCount()});
            }
        }
        PoiTagCounts counts = PoiFootTrafficTagAggregator.aggregate(rows);
        var r =
                FootTrafficSimulationEngine.simulate(
                        counts,
                        request.getPopulationDensity(),
                        request.getAvgIncome(),
                        request.getH3Index(),
                        request.getJitterSalt(),
                        zoneParamsRegistry::resolve);
        SimulateCellResponse res = new SimulateCellResponse();
        res.setArchetype(r.archetype());
        res.setArchetypeConfidence(r.archetypeConfidence());
        res.setBaselineDaily(r.baselineDaily());
        res.setPeakHourly(r.peakHourly());
        res.setNoiseSeed(r.noiseSeed());
        res.setDriverPoiCount(r.driverPoiCount());
        res.setCompetitorPoiCount(r.competitorPoiCount());
        res.setTransitPoiCount(r.transitPoiCount());
        return res;
    }
}
