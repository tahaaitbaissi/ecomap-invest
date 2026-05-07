package com.example.backend.repositories;

import com.example.backend.entities.H3Hexagon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface H3HexagonRepository extends JpaRepository<H3Hexagon, String> {

    /**
     * H3 indices whose boundary intersects the viewport and the Casablanca study polygon ({@code
     * ecomap_study_polygon}). Corner order matches map bbox APIs: {@code swLng, swLat, neLng, neLat}.
     */
    @Query(
            value =
                    "SELECT h.h3_index FROM h3_hexagon h WHERE ST_Intersects(h.boundary, "
                            + "ST_MakeEnvelope(:swLng, :swLat, :neLng, :neLat, 4326)) AND "
                            + "ST_Intersects(h.boundary, "
                            + "(SELECT geom FROM ecomap_study_polygon WHERE id = 1))",
            nativeQuery = true)
    List<String> findH3IndicesIntersectingBbox(
            @Param("swLng") double swLng,
            @Param("swLat") double swLat,
            @Param("neLng") double neLng,
            @Param("neLat") double neLat);

    /**
     * Indices for score normalization bounds (deterministic subset of the seeded grid inside the study
     * polygon).
     */
    @Query(
            value =
                    "SELECT h.h3_index FROM h3_hexagon h WHERE ST_Intersects(h.boundary, "
                            + "(SELECT geom FROM ecomap_study_polygon WHERE id = 1)) "
                            + "ORDER BY md5(h.h3_index) LIMIT :limit",
            nativeQuery = true)
    List<String> findAllH3IndicesLimited(@Param("limit") int limit);
}
