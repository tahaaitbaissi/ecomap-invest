package com.example.backend.repositories;

import com.example.backend.entities.H3Hexagon;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface H3HexagonRepository extends JpaRepository<H3Hexagon, String> {

    // Trouver un hexagone précis par son code H3 (ex: 881f1d4881fffff)
    Optional<H3Hexagon> findByH3Index(String h3Index);

    /**
     * Requête spatiale PostGIS : récupère tous les hexagones qui intersectent 
     * une zone donnée (Bounding Box).
     * ST_Intersects est indispensable pour l'affichage sur la carte.
     */
    @Query("SELECT h FROM H3Hexagon h WHERE ST_Intersects(h.boundary, :bbox) = true")
    List<H3Hexagon> findAllInBBox(@Param("bbox") Geometry bbox);

    // Supprimer tous les hexagones d'une certaine résolution (utile pour le clean-up)
    void deleteByResolution(int resolution);
}