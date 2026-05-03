package com.example.backend.entities;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "h3_hexagons")
public class H3Hexagon {
    @Id
    @Column(name = "h3_index", length = 15)
    private String h3Index;

    // Ajout du SRID 4326 (GPS standard) pour PostGIS
    @Column(columnDefinition = "geometry(Polygon, 4326)")
    private Polygon boundary;

    private int resolution = 9;

    // Getters et Setters (Garde les tiens ou utilise Lombok @Getter @Setter)
    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }
    public Polygon getBoundary() { return boundary; }
    public void setBoundary(Polygon boundary) { this.boundary = boundary; }
    public int getResolution() { return resolution; }
    public void setResolution(int resolution) { this.resolution = resolution; }
}