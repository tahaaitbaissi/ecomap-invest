"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { MapContainer, TileLayer, Marker, Popup, useMapEvents, useMap } from "react-leaflet";
import type { LatLngBounds } from "leaflet";
import L from "leaflet";
import "@/lib/leaflet-icon-fix";
import type { PoiDto } from "@/services/api/poiService";
import type { HexagonDto } from "@/services/api/hexagonService";
import HexagonLayer from "@/components/map/HexagonLayer";
import PoiClusterLayer from "@/components/map/PoiClusterLayer";
import {
  CASA_CENTER,
  INITIAL_ZOOM,
  MAX_BOUNDS,
  MAX_ZOOM,
  MIN_ZOOM,
  clampPointToPilot,
  clampViewportBbox,
} from "@/lib/casablancaMapConstraints";

export interface BoundingBox {
  northEast: { lat: number; lng: number };
  southWest: { lat: number; lng: number };
}

export interface MapViewport {
  bbox: BoundingBox;
  zoom: number;
}

export interface GhostMarkerDto {
  id: string;
  lat: number;
  lng: number;
  type: "DRIVER" | "COMPETITOR" | "OPPORTUNITY";
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
  /** Bbox + zoom for data fetches (zoom drives H3 resolution on the server). */
  onBoundsChange?: (viewport: MapViewport) => void;
  onHexClick?: (hex: HexagonDto) => void;
  onSimulationMapClick?: (lat: number, lng: number) => void;
  flyTo?: { lat: number; lng: number } | null;
}

function MapInteractionGate({ onInteractingChange }: { onInteractingChange: (busy: boolean) => void }) {
  const settleRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const scheduleSettle = useCallback(() => {
    if (settleRef.current) clearTimeout(settleRef.current);
    settleRef.current = setTimeout(() => {
      onInteractingChange(false);
      settleRef.current = null;
    }, 110);
  }, [onInteractingChange]);

  useMapEvents({
    zoomstart() {
      if (settleRef.current) clearTimeout(settleRef.current);
      onInteractingChange(true);
    },
    zoomend() {
      scheduleSettle();
    },
  });

  useEffect(
    () => () => {
      if (settleRef.current) clearTimeout(settleRef.current);
    },
    [],
  );

  return null;
}

function BoundsTracker({ onBoundsChange }: { onBoundsChange?: (viewport: MapViewport) => void }) {
  const lastViewportKeyRef = useRef<string | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const emit = useCallback(
    (bounds: LatLngBounds, zoom: number) => {
      if (!onBoundsChange) return;
      const bbox = clampViewportBbox({
        northEast: { lat: bounds.getNorthEast().lat, lng: bounds.getNorthEast().lng },
        southWest: { lat: bounds.getSouthWest().lat, lng: bounds.getSouthWest().lng },
      });
      const key =
        [
          zoom,
          bbox.southWest.lat.toFixed(5),
          bbox.southWest.lng.toFixed(5),
          bbox.northEast.lat.toFixed(5),
          bbox.northEast.lng.toFixed(5),
        ].join(":");
      if (lastViewportKeyRef.current === key) return;
      lastViewportKeyRef.current = key;
      onBoundsChange({ bbox, zoom });
    },
    [onBoundsChange],
  );

  const map = useMapEvents({
    moveend() {
      scheduleEmit();
    },
    zoomend() {
      scheduleEmit();
    },
  });

  const scheduleEmit = useCallback(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      emit(map.getBounds(), map.getZoom());
    }, 280);
  }, [emit, map]);

  useEffect(() => {
    scheduleEmit();
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [scheduleEmit]);

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
    const [lat, lng] = clampPointToPilot(flyTo.lat, flyTo.lng);
    map.flyTo([lat, lng], 16);
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
  const [mapBusy, setMapBusy] = useState(false);

  const hexagonsToRender = mapBusy ? [] : hexagons ?? [];

  return (
    <MapContainer
      center={CASA_CENTER}
      zoom={INITIAL_ZOOM}
      className="h-full w-full"
      zoomControl
      minZoom={MIN_ZOOM}
      maxZoom={MAX_ZOOM}
      maxBounds={MAX_BOUNDS}
      maxBoundsViscosity={0.95}
      preferCanvas
      style={simClick ? { cursor: "crosshair" } : undefined}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <MapInteractionGate onInteractingChange={setMapBusy} />
      <BoundsTracker onBoundsChange={onBoundsChange} />
      <FlyToHandler flyTo={flyTo} />
      <SimulationClickLayer enabled={simClick} onClick={onSimulationMapClick} />
      {showHeatmap && !!hexagonsToRender.length && (
        <HexagonLayer
          hexagons={hexagonsToRender}
          selectedHexIndex={selectedHexIndex}
          showScoreLabels={showScoreLabels}
          onHexClick={onHexClick}
        />
      )}
      <PoiClusterLayer pois={pois} enabled={showPoiMarkers} />
      {ghostMarkers.map((g) => (
        <Marker
          key={g.id}
          position={[g.lat, g.lng]}
          icon={L.divIcon({
            className: "ghost-marker-icon",
            html: `<div style="width:18px;height:18px;border-radius:50%;background:${
              g.type === "DRIVER"
                ? "#22c55e"
                : g.type === "OPPORTUNITY"
                  ? "#8b5cf6"
                  : "#ef4444"
            };border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.4);display:flex;align-items:center;justify-content:center;color:#fff;font-size:11px;font-weight:700">${g.type === "DRIVER" ? "+" : g.type === "OPPORTUNITY" ? "?" : "×"}</div>`,
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
