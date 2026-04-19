"use client";

import { useEffect, useRef } from "react";
import { MapContainer, TileLayer, Marker, Popup, useMapEvents, useMap } from "react-leaflet";
import L from "leaflet";
import type { LatLngBounds } from "leaflet";
import "@/lib/leaflet-icon-fix";
import type { PoiDto } from "@/services/api/poiService";
import type { HexagonDto } from "@/services/api/hexagonService";
import type { GhostMarker } from "@/store/useStore";
import HexagonLayer from "@/components/map/HexagonLayer";

const CASABLANCA: [number, number] = [33.5731, -7.5898];
const DEFAULT_ZOOM = 13;

export interface BoundingBox {
  northEast: { lat: number; lng: number };
  southWest: { lat: number; lng: number };
}

export interface MapViewerProps {
  pois?: PoiDto[];
  hexagons?: HexagonDto[];
  selectedHexIndex?: string | null;
  ghostMarkers?: GhostMarker[];
  isSimulationActive?: boolean;
  showHeatmap?: boolean;
  showScoreLabels?: boolean;
  showPoiMarkers?: boolean;
  onBoundsChange?: (bbox: BoundingBox) => void;
  onHexClick?: (hex: HexagonDto) => void;
  onMapClick?: (lat: number, lng: number) => void;
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
    moveend() { emit(map.getBounds()); },
    zoomend() { emit(map.getBounds()); },
  });

  // Leaflet ne déclenche pas moveend au premier rendu — fetch initial forcé
  useEffect(() => {
    emit(map.getBounds());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return null;
}

function FlyToHandler({ flyTo }: { flyTo?: { lat: number; lng: number } | null }) {
  const map = useMap();
  const lastKey = useRef<string | null>(null);
  useEffect(() => {
    if (!flyTo) { lastKey.current = null; return; }
    const key = `${flyTo.lat},${flyTo.lng}`;
    if (lastKey.current === key) return;
    lastKey.current = key;
    map.flyTo([flyTo.lat, flyTo.lng], 16);
  }, [flyTo, map]);
  return null;
}

function MapClickTracker({ isActive, onMapClick }: { isActive: boolean; onMapClick?: (lat: number, lng: number) => void }) {
  useMapEvents({
    click(e) {
      if (isActive && onMapClick) onMapClick(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
}

function GhostMarkerLayer({ markers }: { markers: GhostMarker[] }) {
  return (
    <>
      {markers.map((m) => {
        const isComp = m.type === "competitor";
        const icon = L.divIcon({
          html: `<div style="width:22px;height:22px;border-radius:50%;background:${isComp ? "#ef4444" : "#22c55e"};display:flex;align-items:center;justify-content:center;color:white;font-size:14px;font-weight:bold;border:2.5px solid white;box-shadow:0 2px 8px rgba(0,0,0,0.35)">${isComp ? "✕" : "+"}</div>`,
          className: "",
          iconSize: [22, 22],
          iconAnchor: [11, 11],
        });
        return (
          <Marker key={m.id} position={[m.lat, m.lng]} icon={icon}>
            <Popup>
              <span style={{ fontSize: "11px" }}>
                <strong>{m.type}</strong>: {m.tag}
              </span>
            </Popup>
          </Marker>
        );
      })}
    </>
  );
}

export default function MapViewer({
  pois,
  hexagons,
  selectedHexIndex,
  ghostMarkers = [],
  isSimulationActive = false,
  showHeatmap = true,
  showScoreLabels = true,
  showPoiMarkers = true,
  onBoundsChange,
  onHexClick,
  onMapClick,
  flyTo,
}: MapViewerProps) {
  return (
    <div style={{ height: "100%", width: "100%", cursor: isSimulationActive ? "crosshair" : undefined }}>
      <MapContainer center={CASABLANCA} zoom={DEFAULT_ZOOM} className="h-full w-full" zoomControl>
        <TileLayer
          attribution="Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community"
          url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
        />
        <BoundsTracker onBoundsChange={onBoundsChange} />
        <FlyToHandler flyTo={flyTo} />
        <MapClickTracker isActive={isSimulationActive} onMapClick={onMapClick} />
        {showHeatmap && hexagons && hexagons.length > 0 && (
          <HexagonLayer
            hexagons={hexagons}
            selectedHexIndex={selectedHexIndex}
            showScoreLabels={showScoreLabels}
            onHexClick={onHexClick}
          />
        )}
        {ghostMarkers.length > 0 && <GhostMarkerLayer markers={ghostMarkers} />}
        {showPoiMarkers &&
          pois?.map((poi) => (
            <Marker key={poi.id} position={[poi.latitude, poi.longitude]}>
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
    </div>
  );
}
