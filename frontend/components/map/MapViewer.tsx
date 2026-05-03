"use client";

import { useEffect, useRef } from "react";
import { MapContainer, TileLayer, Marker, Popup, useMapEvents, useMap } from "react-leaflet";
import type { LatLngBounds } from "leaflet";
import L from "leaflet";
import "@/lib/leaflet-icon-fix";
import type { PoiDto } from "@/services/api/poiService";
import type { HexagonDto } from "@/services/api/hexagonService";
import HexagonLayer from "@/components/map/HexagonLayer";

const CASABLANCA: [number, number] = [33.5731, -7.5898];
const DEFAULT_ZOOM = 13;

export interface BoundingBox {
  northEast: { lat: number; lng: number };
  southWest: { lat: number; lng: number };
}

export interface GhostMarkerDto {
  id: string;
  lat: number;
  lng: number;
  type: "DRIVER" | "COMPETITOR";
  tag: string;
}

export interface MapViewerProps {
  pois?: PoiDto[];
  hexagons?: HexagonDto[];
  selectedHexIndex?: string | null;
  ghostMarkers?: GhostMarkerDto[];
  showHeatmap?: boolean;
  showScoreLabels?: boolean;
  showPoiMarkers?: boolean;
  simulationMode?: boolean;
  onBoundsChange?: (bbox: BoundingBox) => void;
  onHexClick?: (hex: HexagonDto) => void;
  onSimulationMapClick?: (lat: number, lng: number) => void;
  flyTo?: { lat: number; lng: number } | null;
}

function BoundsTracker({ onBoundsChange }: { onBoundsChange?: (bbox: BoundingBox) => void }) {
  function emit(bounds: LatLngBounds) {
    if (!onBoundsChange) return;
    onBoundsChange({
      northEast: { lat: bounds.getNorthEast().lat, lng: bounds.getNorthEast().lng },
      southWest: { lat: bounds.getSouthWest().lat, lng: bounds.getSouthWest().lng },
    });
  }

  const map = useMapEvents({
    moveend() {
      emit(map.getBounds());
    },
    zoomend() {
      emit(map.getBounds());
    },
  });

  useEffect(() => {
    emit(map.getBounds());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return null;
}

function SimulationClickLayer({
  enabled,
  onClick,
}: {
  enabled: boolean;
  onClick?: (lat: number, lng: number) => void;
}) {
  useMapEvents({
    click(e) {
      if (!enabled || !onClick) return;
      onClick(e.latlng.lat, e.latlng.lng);
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

export default function MapViewer({
  pois,
  hexagons,
  selectedHexIndex,
  ghostMarkers = [],
  showHeatmap = true,
  showScoreLabels = true,
  showPoiMarkers = true,
  onBoundsChange,
  onHexClick,
  flyTo,
  simulationMode,
  onSimulationMapClick,
}: MapViewerProps) {
  const simClick = !!(simulationMode && onSimulationMapClick);

  return (
    <MapContainer
      center={CASABLANCA}
      zoom={DEFAULT_ZOOM}
      className="h-full w-full"
      zoomControl
      style={simClick ? { cursor: "crosshair" } : undefined}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <BoundsTracker onBoundsChange={onBoundsChange} />
      <FlyToHandler flyTo={flyTo} />
      <SimulationClickLayer enabled={simClick} onClick={onSimulationMapClick} />
      {showHeatmap && !!hexagons?.length && (
        <HexagonLayer
          hexagons={hexagons}
          selectedHexIndex={selectedHexIndex}
          showScoreLabels={showScoreLabels}
          onHexClick={onHexClick}
        />
      )}
      {showPoiMarkers &&
        pois?.map((poi, index) => (
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
      {ghostMarkers.map((g) => (
        <Marker
          key={g.id}
          position={[g.lat, g.lng]}
          icon={L.divIcon({
            className: "ghost-marker-icon",
            html: `<div style="width:16px;height:16px;border-radius:50%;background:${g.type === "DRIVER" ? "#22c55e" : "#ef4444"};border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.4);display:flex;align-items:center;justify-content:center;color:#fff;font-size:11px;font-weight:700">${g.type === "DRIVER" ? "+" : "×"}</div>`,
            iconSize: [16, 16],
            iconAnchor: [8, 8],
          })}
        >
          <Popup>
            <span className="text-xs font-semibold">{g.type}</span>
            <br />
            <span className="text-xs text-gray-600">{g.tag}</span>
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
