package com.example.backend.controllers;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/actuator")
public class HealthController {

    @Value("${app.rmi.scoring.host:localhost}")
    private String host;

    @Value("${app.rmi.scoring.port:1099}")
    private int port;

    @Value("${app.rmi.scoring.service-name:ScoringService}")
    private String serviceName;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> components = new HashMap<>();
        root.put("components", components);

        Map<String, Object> rmi = new HashMap<>();
        rmi.put("details", Map.of("host", host, "port", port, "serviceName", serviceName));

        boolean ok = false;
        try {
            Registry reg = LocateRegistry.getRegistry(host, port);
            reg.list();
            ok = true;
        } catch (Exception e) {
            rmi.put("error", e.getClass().getSimpleName());
            rmi.put("message", e.getMessage());
        }

        rmi.put("status", ok ? "UP" : "DOWN");
        components.put("rmiRegistry", rmi);

        root.put("status", ok ? "UP" : "DOWN");
        return ResponseEntity.status(ok ? 200 : 503).body(root);
    }
}

