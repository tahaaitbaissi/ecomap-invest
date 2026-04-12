package com.example.ecomap.rmi.scoring;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote scoring service (Java RMI). Implements a saturation-style formula aligned with the EcoMap
 * cahier des charges narrative: drivers (positive), population density (positive), competitors (negative).
 */
public interface ScoringRemote extends Remote {

    /**
     * @param drivers            count of positive location drivers (schools, offices, etc.)
     * @param competitors        count of competing POIs
     * @param densityNormalized  population / traffic density in [0, 1] (caller clamps)
     * @return score in [0, 100]
     */
    double computeSaturationScore(int drivers, int competitors, double densityNormalized) throws RemoteException;

    /** Simple health check for demos. */
    String ping() throws RemoteException;
}
