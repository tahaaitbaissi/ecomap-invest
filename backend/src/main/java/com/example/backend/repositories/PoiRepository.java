package com.example.backend.repositories;

import com.example.backend.entities.Poi;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PoiRepository extends JpaRepository<Poi, UUID> {
    Optional<Poi> findByOsmId(String osmId);

    @Query(value = """
            SELECT *
            FROM poi p
            WHERE ST_Intersects(
                p.location,
                ST_MakeEnvelope(:swLng, :swLat, :neLng, :neLat, 4326)
            )
            """, nativeQuery = true)
    List<Poi> findAllInBoundingBox(@Param("swLng") double swLng,
                                   @Param("swLat") double swLat,
                                   @Param("neLng") double neLng,
                                   @Param("neLat") double neLat);
}
