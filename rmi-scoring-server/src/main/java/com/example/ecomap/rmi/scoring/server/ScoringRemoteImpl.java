package com.example.ecomap.rmi.scoring.server;

import com.example.ecomap.rmi.scoring.ScoringRemote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Server-side saturation score: raw = (D × 0.5) + (P × 0.3 × 100) − (C × 1.2) with P = densityNormalized,
 * then clamped to [0, 100]. Matches the EcoMap CDC-style weighting for demos.
 */
public class ScoringRemoteImpl extends UnicastRemoteObject implements ScoringRemote {

    /**
     * @param exportPort TCP port for RMI object export (must be published in Docker alongside registry port).
     */
    public ScoringRemoteImpl(int exportPort) throws RemoteException {
        super(exportPort);
    }

    @Override
    public double computeSaturationScore(int drivers, int competitors, double densityNormalized) throws RemoteException {
        double p = Math.max(0.0, Math.min(1.0, densityNormalized));
        double raw = (drivers * 0.5d) + (p * 0.3d * 100.0d) - (competitors * 1.2d);
        return Math.max(0.0, Math.min(100.0, raw));
    }

    @Override
    public String ping() throws RemoteException {
        return "EcoMap RMI scoring node OK";
    }
}
