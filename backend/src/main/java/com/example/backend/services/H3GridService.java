package com.example.backend.services;

import com.example.backend.entities.H3Hexagon;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class H3GridService {
    private final H3Core h3Core;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public H3GridService() throws IOException {
        this.h3Core = H3Core.newInstance();
    }

    /**
     * F6.1.1 & F6.1.2 : Génère tous les hexagones pour une zone (BBox) donnée.
     * Utile pour l'initialisation de la base de données.
     */
    public List<H3Hexagon> generateHexagonsForBBox(
            double minLat, double minLng, double maxLat, double maxLng, int resolution) {

        // Définir le rectangle de la zone
        List<LatLng> bbox = List.of(
            new LatLng(minLat, minLng), new LatLng(maxLat, minLng),
            new LatLng(maxLat, maxLng), new LatLng(minLat, maxLng),
            new LatLng(minLat, minLng)
        );

        // Récupérer tous les index H3 couvrant cette zone
        List<String> h3Indexes = h3Core.polygonToCells(bbox, List.of(), resolution);
        List<H3Hexagon> hexagons = new ArrayList<>();
        
        for (String h3Index : h3Indexes) {
            hexagons.add(mapToEntity(h3Index, resolution));
        }
        return hexagons;
    }

    /**
     * Transforme un index H3 String en Entité JPA avec une géométrie PostGIS.
     */
    public H3Hexagon mapToEntity(String h3Index, int resolution) {
        List<LatLng> boundary = h3Core.cellToBoundary(h3Index);
        Coordinate[] coords = new Coordinate[boundary.size() + 1];
        
        for (int i = 0; i < boundary.size(); i++) {
            // H3 utilise (lat, lng), JTS Coordinate utilise (x=lng, y=lat)
            coords[i] = new Coordinate(boundary.get(i).lng, boundary.get(i).lat);
        }
        coords[boundary.size()] = coords[0]; // Fermeture du polygone

        H3Hexagon hex = new H3Hexagon();
        hex.setH3Index(h3Index);
        
        // Création du polygone compatible PostGIS
        Polygon polygon = geometryFactory.createPolygon(coords);
        hex.setBoundary(polygon);
        hex.setResolution(resolution);
        
        return hex;
    }
}