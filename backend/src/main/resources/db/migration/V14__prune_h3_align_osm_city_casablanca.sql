-- Tighten pilot grid to administrative city envelope from OpenStreetMap (Nominatim bbox /
-- OGR GetEnvelope-equivalent). Source: boundary relation for Casablanca city (Morocco).
-- PostGIS envelope order: ST_MakeEnvelope(swLng, swLat, neLng, neLat, 4326)
-- Align with application.yml app.hexagon.study-area defaults after this revision.

DELETE FROM hexagon_score
WHERE h3_index IN (
    SELECT h3_index FROM h3_hexagon
    WHERE NOT ST_Intersects(
        boundary,
        ST_MakeEnvelope(-7.751238, 33.493342, -7.4574165, 33.6409103, 4326)
    )
);

DELETE FROM demographics
WHERE h3_index IN (
    SELECT h3_index FROM h3_hexagon
    WHERE NOT ST_Intersects(
        boundary,
        ST_MakeEnvelope(-7.751238, 33.493342, -7.4574165, 33.6409103, 4326)
    )
);

DELETE FROM h3_hexagon
WHERE NOT ST_Intersects(boundary, ST_MakeEnvelope(-7.751238, 33.493342, -7.4574165, 33.6409103, 4326));
