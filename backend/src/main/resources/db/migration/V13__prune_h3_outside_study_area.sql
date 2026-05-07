-- Remove offshore / out-of-pilot hex rows. Envelope MUST match defaults in application.yml app.hexagon.study-area.
-- Corner order matches PostGIS ST_MakeEnvelope(swLng, swLat, neLng, neLat, 4326).

DELETE FROM hexagon_score
WHERE h3_index IN (
    SELECT h3_index FROM h3_hexagon
    WHERE NOT ST_Intersects(
        boundary,
        ST_MakeEnvelope(-7.58, 33.38, -7.22, 33.77, 4326)
    )
);

DELETE FROM demographics
WHERE h3_index IN (
    SELECT h3_index FROM h3_hexagon
    WHERE NOT ST_Intersects(
        boundary,
        ST_MakeEnvelope(-7.58, 33.38, -7.22, 33.77, 4326)
    )
);

DELETE FROM h3_hexagon
WHERE NOT ST_Intersects(boundary, ST_MakeEnvelope(-7.58, 33.38, -7.22, 33.77, 4326));
