package com.example.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Polygon;

@Getter
@Setter
@Entity
@Table(name = "h3_hexagon")
public class H3Hexagon {

    @Id
    @Column(name = "h3_index", length = 15, nullable = false)
    private String h3Index;

    @Column(name = "boundary", nullable = false, columnDefinition = "geometry(POLYGON,4326)")
    private Polygon boundary;

    @Column(name = "resolution", nullable = false)
    private int resolution = 9;
}
