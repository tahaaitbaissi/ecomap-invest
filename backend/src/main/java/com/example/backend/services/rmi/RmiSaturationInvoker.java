package com.example.backend.services.rmi;

import com.example.ecomap.rmi.scoring.ScoringRemote;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Synchronous RMI calls to {@link ScoringRemote} without Resilience4j.
 * Used by {@link RmiScoringClient} (REST demos and distributed scoring pipeline with Resilience4j fallbacks).
 */
@Slf4j
@Component
public class RmiSaturationInvoker {

    @Value("${app.rmi.scoring.host:localhost}")
    private String host;

    @Value("${app.rmi.scoring.port:1099}")
    private int port;

    @Value("${app.rmi.scoring.service-name:ScoringService}")
    private String serviceName;

    private volatile ScoringRemote cached;

    public double invokeComputeSaturationScore(int drivers, int competitors, double densityNormalized)
            throws RemoteException, NotBoundException {
        ScoringRemote remote = getRemote();
        double score = remote.computeSaturationScore(drivers, competitors, densityNormalized);
        log.debug("RMI scoring completed: drivers={}, competitors={}, density={}, score={}",
                drivers, competitors, densityNormalized, score);
        return score;
    }

    public String invokePing() throws RemoteException, NotBoundException {
        String result = getRemote().ping();
        log.debug("RMI ping successful: {}", result);
        return result;
    }

    private ScoringRemote getRemote() throws RemoteException, NotBoundException {
        ScoringRemote local = cached;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (cached == null) {
                Registry registry = LocateRegistry.getRegistry(host, port);
                cached = (ScoringRemote) registry.lookup(serviceName);
                log.info("RMI stub resolved: {} @ {}:{}", serviceName, host, port);
            }
            return cached;
        }
    }

    public void resetStub() {
        synchronized (this) {
            cached = null;
        }
        log.info("RMI stub reset, will re-lookup on next request");
    }
}
