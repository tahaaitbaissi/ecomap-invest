package com.example.backend.repositories;

import com.example.backend.entities.Simulation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SimulationRepository extends JpaRepository<Simulation, UUID> {
    List<Simulation> findByUserId(String userId);
}