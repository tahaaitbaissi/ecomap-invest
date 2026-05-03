package com.example.backend.repositories;

import com.example.backend.entities.H3Hexagon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface H3HexagonRepository extends JpaRepository<H3Hexagon, String> {

    /**
     * H3 indices whose stored boundary intersects the viewport envelope (WGS84), using the same
     * corner order as map APIs: swLng, swLat, neLng, neLat.
     */
    @Query(
            value =
                    "SELECT h.h3_index FROM h3_hexagon h WHERE ST_Intersects(h.boundary, "
                            + "ST_MakeEnvelope(:swLng, :swLat, :neLng, :neLat, 4326))",
            nativeQuery = true)
    List<String> findH3IndicesIntersectingBbox(
            @Param("swLng") double swLng,
            @Param("swLat") double swLat,
            @Param("neLng") double neLng,
            @Param("neLat") double neLat);
}
