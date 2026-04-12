package com.example.ecomap.rmi.scoring.server;

import com.example.ecomap.rmi.scoring.ScoringRemote;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Starts an RMI registry and binds {@link ScoringRemote} as {@value #DEFAULT_SERVICE_NAME}.
 * <p>
 * System properties / env:
 * <ul>
 *   <li>{@code java.rmi.server.hostname} — required for Docker/LAN (e.g. {@code rmi-scoring} or host IP)</li>
 *   <li>{@code RMI_REGISTRY_PORT} — default 1099</li>
 *   <li>{@code RMI_SERVICE_NAME} — default {@value #DEFAULT_SERVICE_NAME}</li>
 * </ul>
 */
public final class ScoringServerMain {

    public static final String DEFAULT_SERVICE_NAME = "ScoringService";

    public static void main(String[] args) throws RemoteException, AlreadyBoundException {
        int port = parsePort(System.getenv("RMI_REGISTRY_PORT"), 1099);
        String serviceName = System.getenv().getOrDefault("RMI_SERVICE_NAME", DEFAULT_SERVICE_NAME);
        String hostname = System.getProperty("java.rmi.server.hostname", "localhost");

        int exportPort = parsePort(System.getenv("RMI_EXPORT_PORT"), 45000);
        Registry registry = LocateRegistry.createRegistry(port);
        ScoringRemote impl = new ScoringRemoteImpl(exportPort);
        registry.bind(serviceName, impl);

        System.out.println("RMI scoring service '" + serviceName + "' bound on registry port " + port);
        System.out.println("java.rmi.server.hostname=" + hostname);
        System.out.println("object export port=" + exportPort);
    }

    private static int parsePort(String env, int defaultPort) {
        if (env == null || env.isBlank()) {
            return defaultPort;
        }
        try {
            return Integer.parseInt(env.trim());
        } catch (NumberFormatException e) {
            return defaultPort;
        }
    }
}
