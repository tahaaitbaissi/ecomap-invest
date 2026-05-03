package com.example.backend.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Data
public class Simulation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private String userId;
    private Double lat;
    private Double lng;
    private String businessType;
    private Double calculatedScore;
    private LocalDateTime createdAt = LocalDateTime.now();
}