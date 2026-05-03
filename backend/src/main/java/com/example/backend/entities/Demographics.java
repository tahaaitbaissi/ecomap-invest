package com.example.backend.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "demographics")
public class Demographics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "h3_index", length = 15)
    private String h3Index;

    private Double populationDensity;
    private Double avgIncome;

    // Getters et Setters
    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }
    public Double getPopulationDensity() { return populationDensity; }
    public void setPopulationDensity(Double v) { this.populationDensity = v; }
    public Double getAvgIncome() { return avgIncome; }
    public void setAvgIncome(Double v) { this.avgIncome = v; }
}