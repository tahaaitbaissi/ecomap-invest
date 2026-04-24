package com.example.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "demographics")
public class Demographics {

    @Id
    @Column(name = "h3_index", length = 32)
    private String h3Index;

    @Column(name = "population_density")
    private Double populationDensity;

    @Column(name = "avg_income")
    private Double avgIncome;
}
