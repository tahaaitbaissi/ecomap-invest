package com.example.backend.foottraffic.services;

import com.ecomap.foottraffic.simulation.FootTrafficSimulationEngine;
import com.ecomap.ftsim.ws.xml.SimulateCellRequest;
import com.ecomap.ftsim.ws.xml.SimulateCellResponse;
import com.example.backend.config.FootTrafficSoapExecutorConfiguration;
import com.example.backend.foottraffic.config.FootTrafficProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Remote SOAP foot-traffic simulation with Resilience4j (circuit breaker, retry, time limiter).
 * Fallback returns {@code null} so {@link FootTrafficRecomputeService} uses in-process simulation.
 */
@Slf4j
@Service
public class FootTrafficSoapSimulationClient {

    private final WebServiceTemplate webServiceTemplate;
    private final FootTrafficProperties properties;
    private final Executor soapExecutor;

    public FootTrafficSoapSimulationClient(
            @Autowired(required = false) WebServiceTemplate footTrafficWebServiceTemplate,
            FootTrafficProperties properties,
            @Qualifier(FootTrafficSoapExecutorConfiguration.FOOT_TRAFFIC_SOAP_EXECUTOR) Executor soapExecutor) {
        this.webServiceTemplate = footTrafficWebServiceTemplate;
        this.properties = properties;
        this.soapExecutor = soapExecutor;
    }

    public boolean isReady() {
        return properties.getSoap().isEnabled() && webServiceTemplate != null;
    }

    @CircuitBreaker(name = "footTrafficSoap", fallbackMethod = "fallbackSimulate")
    @Retry(name = "footTrafficSoap", fallbackMethod = "fallbackSimulate")
    @TimeLimiter(name = "footTrafficSoap", fallbackMethod = "fallbackSimulateFuture")
    public CompletableFuture<FootTrafficSimulationEngine.CellSimulationResult> simulateCellAsync(
            SimulateCellRequest request) {
        if (!isReady()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> invoke(request), soapExecutor);
    }

    private FootTrafficSimulationEngine.CellSimulationResult invoke(SimulateCellRequest request) {
        Object out = webServiceTemplate.marshalSendAndReceive(request);
        if (!(out instanceof SimulateCellResponse res)) {
            throw new IllegalStateException("unexpected SOAP payload: " + out);
        }
        return new FootTrafficSimulationEngine.CellSimulationResult(
                res.getArchetype(),
                res.getArchetypeConfidence(),
                res.getBaselineDaily(),
                res.getPeakHourly(),
                res.getNoiseSeed(),
                res.getDriverPoiCount(),
                res.getCompetitorPoiCount(),
                res.getTransitPoiCount());
    }

    @SuppressWarnings("unused")
    private CompletableFuture<FootTrafficSimulationEngine.CellSimulationResult> fallbackSimulateFuture(
            SimulateCellRequest request, Throwable t) {
        return fallbackSimulate(request, t);
    }

    @SuppressWarnings("unused")
    private CompletableFuture<FootTrafficSimulationEngine.CellSimulationResult> fallbackSimulate(
            SimulateCellRequest request, Throwable t) {
        log.warn(
                "SOAP foot-traffic cell simulation unavailable, using local engine. Cause: {}",
                t != null ? t.getMessage() : "null");
        return CompletableFuture.completedFuture(null);
    }
}
