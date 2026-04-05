"use client";

import { MapContainer, TileLayer, useMapEvents } from "react-leaflet";
import type { LatLngBounds } from "leaflet";
import "@/lib/leaflet-icon-fix";

const CASABLANCA: [number, number] = [33.5731, -7.5898];
const DEFAULT_ZOOM = 13;

export interface BoundingBox {
  northEast: { lat: number; lng: number };
  southWest: { lat: number; lng: number };
}

export interface MapViewerProps {
  onBoundsChange?: (bbox: BoundingBox) => void;
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

export default function MapViewer({ onBoundsChange }: MapViewerProps) {
  return (
    <MapContainer center={CASABLANCA} zoom={DEFAULT_ZOOM} className="h-full w-full" zoomControl>
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <BoundsTracker onBoundsChange={onBoundsChange} />
    </MapContainer>
  );
}
