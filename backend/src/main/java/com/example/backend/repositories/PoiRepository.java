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

    boolean existsByOsmId(String osmId);

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

    /**
     * Find all POIs within a radius (in kilometers) of a given location.
     * @param latitude center latitude (WGS84)
     * @param longitude center longitude (WGS84)
     * @param radiusKm search radius in kilometers
     * @return POIs within the radius
     */
    @Query(value = """
            SELECT *
            FROM poi p
            WHERE ST_DWithin(
                p.location::geography,
                ST_Point(:longitude, :latitude)::geography,
                :radiusMeters
            )
            """, nativeQuery = true)
    List<Poi> findNearbyPois(@Param("latitude") double latitude,
                             @Param("longitude") double longitude,
                             @Param("radiusMeters") double radiusMeters);

    /**
     * Count POIs with a specific typeTag within a radius of a given location.
     * @param typeTag the POI category (e.g., "category=school")
     * @param latitude center latitude (WGS84)
     * @param longitude center longitude (WGS84)
     * @param radiusMeters search radius in meters
     * @return count of matching POIs
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM poi p
            WHERE p.type_tag = :typeTag
            AND ST_DWithin(
                p.location::geography,
                ST_Point(:longitude, :latitude)::geography,
                :radiusMeters
            )
            """, nativeQuery = true)
    long countByTypeTagAndNearby(@Param("typeTag") String typeTag,
                                 @Param("latitude") double latitude,
                                 @Param("longitude") double longitude,
                                 @Param("radiusMeters") double radiusMeters);

    /**
     * Count all POIs (any type) within a radius of a given location.
     * @param latitude center latitude (WGS84)
     * @param longitude center longitude (WGS84)
     * @param radiusMeters search radius in meters
     * @return count of nearby POIs
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM poi p
            WHERE ST_DWithin(
                p.location::geography,
                ST_Point(:longitude, :latitude)::geography,
                :radiusMeters
            )
            """, nativeQuery = true)
    long countAllNearby(@Param("latitude") double latitude,
                        @Param("longitude") double longitude,
                        @Param("radiusMeters") double radiusMeters);
}
