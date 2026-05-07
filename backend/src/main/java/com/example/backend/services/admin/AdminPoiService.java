package com.example.backend.services.admin;

import com.example.backend.controllers.dto.AdminPoiResponse;
import com.example.backend.controllers.dto.AdminPoiUpsertRequest;
import com.example.backend.entities.Poi;
import com.example.backend.repositories.PoiRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPoiService {

    private static final GeometryFactory GEOM =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final PoiRepository poiRepository;
    private final ScoreCacheVersionService scoreCacheVersionService;

    @Transactional(readOnly = true)
    public Page<AdminPoiResponse> searchPage(
            Double minX,
            Double minY,
            Double maxX,
            Double maxY,
            String typeTag,
            String nameLike,
            Pageable pageable) {
        // Start from a non-null "match all" spec (avoids ambiguous where(null) overloads).
        Specification<Poi> spec = (root, q, cb) -> cb.conjunction();

        if (typeTag != null && !typeTag.isBlank()) {
            String t = typeTag.trim();
            spec = spec.and((root, q, cb) -> cb.equal(root.get("typeTag"), t));
        }
        if (nameLike != null && !nameLike.isBlank()) {
            String needle = "%" + nameLike.trim().toLowerCase() + "%";
            spec =
                    spec.and(
                            (root, q, cb) ->
                                    cb.like(cb.lower(root.get("name")), needle));
        }

        boolean hasBbox = minX != null && minY != null && maxX != null && maxY != null;
        if (hasBbox) {
            spec =
                    spec.and(
                            (root, q, cb) -> {
                                var env =
                                        cb.function(
                                                "ST_MakeEnvelope",
                                                Object.class,
                                                cb.literal(minX),
                                                cb.literal(minY),
                                                cb.literal(maxX),
                                                cb.literal(maxY),
                                                cb.literal(4326));
                                return cb.isTrue(
                                        cb.function(
                                                "ST_Within",
                                                Boolean.class,
                                                root.get("location"),
                                                env));
                            });
        }

        return poiRepository.findAll(spec, pageable).map(AdminPoiService::toDto);
    }

    @Transactional
    public AdminPoiResponse create(AdminPoiUpsertRequest req) {
        validateLatLng(req.lat(), req.lng());
        String osmId = req.osmId();
        if (osmId == null || osmId.isBlank()) {
            osmId = "manual:" + UUID.randomUUID();
        }
        Optional<Poi> existing = poiRepository.findByOsmId(osmId.trim());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("osmId already exists");
        }
        Poi p = new Poi();
        p.setOsmId(osmId.trim());
        p.setName(req.name());
        p.setAddress(req.address());
        p.setTypeTag(req.typeTag().trim());
        p.setLocation(point(req.lat(), req.lng()));
        p.setPriceLevel(req.priceLevel());
        p.setRating(req.rating());
        p.setImportedAt(Timestamp.from(Instant.now()));
        AdminPoiResponse out = toDto(poiRepository.save(p));
        scoreCacheVersionService.bumpPoiVersion();
        return out;
    }

    @Transactional
    public AdminPoiResponse update(UUID id, AdminPoiUpsertRequest req) {
        validateLatLng(req.lat(), req.lng());
        Poi p = poiRepository.findById(id).orElseThrow();
        if (req.osmId() != null && !req.osmId().isBlank() && !req.osmId().trim().equals(p.getOsmId())) {
            if (poiRepository.findByOsmId(req.osmId().trim()).isPresent()) {
                throw new IllegalArgumentException("osmId already exists");
            }
            p.setOsmId(req.osmId().trim());
        }
        if (req.name() != null) p.setName(req.name());
        if (req.address() != null) p.setAddress(req.address());
        if (req.typeTag() != null && !req.typeTag().isBlank()) p.setTypeTag(req.typeTag().trim());
        p.setLocation(point(req.lat(), req.lng()));
        p.setPriceLevel(req.priceLevel());
        p.setRating(req.rating());
        AdminPoiResponse out = toDto(poiRepository.save(p));
        scoreCacheVersionService.bumpPoiVersion();
        return out;
    }

    @Transactional
    public void delete(UUID id) {
        poiRepository.deleteById(id);
        scoreCacheVersionService.bumpPoiVersion();
    }

    private static Point point(double lat, double lng) {
        return GEOM.createPoint(new Coordinate(lng, lat));
    }

    private static void validateLatLng(Double lat, Double lng) {
        if (lat == null || lng == null) {
            throw new IllegalArgumentException("lat/lng required");
        }
        if (!Double.isFinite(lat) || !Double.isFinite(lng)) {
            throw new IllegalArgumentException("lat/lng must be finite");
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new IllegalArgumentException("lat/lng out of range");
        }
    }

    private static AdminPoiResponse toDto(Poi p) {
        return new AdminPoiResponse(
                p.getId(),
                p.getOsmId(),
                p.getName(),
                p.getAddress(),
                p.getTypeTag(),
                p.getLocation().getY(),
                p.getLocation().getX(),
                p.getPriceLevel(),
                p.getRating(),
                p.getImportedAt() != null ? p.getImportedAt().toInstant().toString() : null);
    }
}

