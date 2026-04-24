"use client";

import { useEffect, useRef } from "react";
import { MapContainer, TileLayer, Marker, Popup, Polygon, useMapEvents, useMap } from "react-leaflet";
import type { LatLngBounds } from "leaflet";
import "@/lib/leaflet-icon-fix";
import type { PoiDto } from "@/services/api/poiService";
import type { HexagonDto } from "@/services/api/hexagonService";

const CASABLANCA: [number, number] = [33.5731, -7.5898];
const DEFAULT_ZOOM = 13;

export interface BoundingBox {
  northEast: { lat: number; lng: number };
  southWest: { lat: number; lng: number };
}

export interface MapViewerProps {
  pois?: PoiDto[];
  hexagons?: HexagonDto[];
  onBoundsChange?: (bbox: BoundingBox) => void;
  flyTo?: { lat: number; lng: number } | null;
}

function BoundsTracker({ onBoundsChange }: { onBoundsChange?: (bbox: BoundingBox) => void }) {
  const map = useMapEvents({
    moveend() {
      if (!onBoundsChange) return;
      const bounds: LatLngBounds = map.getBounds();
      onBoundsChange({
        northEast: { lat: bounds.getNorthEast().lat, lng: bounds.getNorthEast().lng },
        southWest: { lat: bounds.getSouthWest().lat, lng: bounds.getSouthWest().lng },
      });
    },
    zoomend() {
      if (!onBoundsChange) return;
      const bounds: LatLngBounds = map.getBounds();
      onBoundsChange({
        northEast: { lat: bounds.getNorthEast().lat, lng: bounds.getNorthEast().lng },
        southWest: { lat: bounds.getSouthWest().lat, lng: bounds.getSouthWest().lng },
      });
    },
  });

  return null;
}

function FlyToHandler({ flyTo }: { flyTo?: { lat: number; lng: number } | null }) {
  const map = useMap();
  const lastKey = useRef<string | null>(null);
  useEffect(() => {
    if (!flyTo) {
      lastKey.current = null;
      return;
    }
    const key = `${flyTo.lat},${flyTo.lng}`;
    if (lastKey.current === key) return;
    lastKey.current = key;
    map.flyTo([flyTo.lat, flyTo.lng], 16);
  }, [flyTo, map]);
  return null;
}

function hexColor(score: number): string {
  if (score < 30) return "#ef4444";
  if (score < 60) return "#eab308";
  return "#22c55e";
}

export default function MapViewer({ pois, hexagons, onBoundsChange, flyTo }: MapViewerProps) {
  return (
    <MapContainer center={CASABLANCA} zoom={DEFAULT_ZOOM} className="h-full w-full" zoomControl>
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <BoundsTracker onBoundsChange={onBoundsChange} />
      <FlyToHandler flyTo={flyTo} />
      {hexagons?.map((h) => (
        <Polygon
          key={h.h3Index}
          positions={h.boundary.map((p) => [p.lat, p.lng] as [number, number])}
          pathOptions={{
            color: hexColor(h.score),
            fillColor: hexColor(h.score),
            fillOpacity: 0.35,
            weight: 1,
          }}
        />
      ))}
      {pois?.map((poi, index) => (
        <Marker key={poi.id || `poi-${index}`} position={[poi.latitude, poi.longitude]}>
          <Popup>
            <strong>{poi.name ?? "Unknown"}</strong>
            <br />
            <span className="text-xs text-gray-500">{poi.typeTag}</span>
            {poi.address && (
              <>
                <br />
                <span className="text-xs">{poi.address}</span>
              </>
            )}
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
