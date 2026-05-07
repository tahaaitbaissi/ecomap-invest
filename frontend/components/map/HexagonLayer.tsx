"use client";

import React, { useMemo, useState, useEffect } from "react";
import { Polygon, Tooltip, Marker, useMap } from "react-leaflet";
import L from "leaflet";
import type { HexagonDto } from "@/services/api/hexagonService";
import { getHexColor, getHexOpacity } from "@/lib/hexagonUtils";

function boundaryKey(b: { lat: number; lng: number }[]): string {
  if (!b?.length) return "";
  return b.map((p) => `${p.lat.toFixed(6)},${p.lng.toFixed(6)}`).join(";");
}

/** Leaflet expects [lat, lng]; H3 ring should be explicitly closed for crisp edges at all zoom levels. */
function ringLatLngs(boundary: { lat: number; lng: number }[]): [number, number][] {
  if (!boundary.length) return [];
  const pts = boundary.map((p) => [p.lat, p.lng] as [number, number]);
  const first = pts[0];
  const last = pts[pts.length - 1];
  if (first[0] !== last[0] || first[1] !== last[1]) {
    return [...pts, first];
  }
  return pts;
}

function HexScoreLabel({
  center,
  score,
  show,
}: {
  center: [number, number];
  score: number | null;
  show: boolean;
}) {
  const map = useMap();
  const [zoom, setZoom] = useState(() => map.getZoom());

  useEffect(() => {
    const onZoom = () => setZoom(map.getZoom());
    map.on("zoom zoomend", onZoom);
    return () => {
      map.off("zoom zoomend", onZoom);
    };
  }, [map]);

  if (!show || score == null) return null;
  if (zoom < 13) return null;

  const fontPx = zoom >= 15 ? 12 : zoom >= 13 ? 11 : 10;
  const scoreIcon = L.divIcon({
    html: `<span style="font-size:${fontPx}px;font-weight:700;color:#fff;text-shadow:0 1px 3px rgba(0,0,0,0.75);pointer-events:none;user-select:none;white-space:nowrap">${Math.round(score)}</span>`,
    className: "",
    iconSize: [32, 18],
    iconAnchor: [16, 9],
  });

  return <Marker position={center} icon={scoreIcon} interactive={false} />;
}

interface HexItemProps {
  hex: HexagonDto;
  isSelected: boolean;
  showScoreLabels: boolean;
  /** Sticky tooltips are expensive at low zoom; disable when zoomed out. */
  hexStickyTooltips: boolean;
  onHexClick?: (h: HexagonDto) => void;
}

const HexItem = React.memo(
  function HexItem({
    hex,
    isSelected,
    showScoreLabels,
    hexStickyTooltips,
    onHexClick,
  }: HexItemProps) {
    const color = getHexColor(hex.score);
    const opacity = getHexOpacity(hex.score);

    const positions = useMemo(() => ringLatLngs(hex.boundary), [hex.boundary]);

    const center = useMemo((): [number, number] => {
      const b = hex.boundary;
      if (!b.length) return [0, 0];
      const lat = b.reduce((s, p) => s + p.lat, 0) / b.length;
      const lng = b.reduce((s, p) => s + p.lng, 0) / b.length;
      return [lat, lng];
    }, [hex.boundary]);

    if (positions.length < 3) return null;

    return (
      <>
        <Polygon
          positions={positions}
          pathOptions={{
            fillColor: color,
            color: isSelected ? "#ffffff" : "#555555",
            fillOpacity: isSelected ? Math.min(opacity + 0.15, 0.92) : opacity,
            weight: isSelected ? 2.5 : 0.9,
            lineJoin: "round",
            lineCap: "round",
          }}
          eventHandlers={{ click: () => onHexClick?.(hex) }}
        >
          <Tooltip sticky={hexStickyTooltips} direction="top" opacity={0.95}>
            <span style={{ fontWeight: 600 }}>
              Score : {hex.score == null ? "—" : Math.round(hex.score)}
            </span>
            <br />
            <span style={{ fontSize: "10px", color: "#666" }}>{hex.h3Index.slice(-10)}</span>
          </Tooltip>
        </Polygon>
        <HexScoreLabel center={center} score={hex.score} show={showScoreLabels} />
      </>
    );
  },
  (prev, next) =>
    prev.hex.h3Index === next.hex.h3Index &&
    prev.hex.score === next.hex.score &&
    boundaryKey(prev.hex.boundary) === boundaryKey(next.hex.boundary) &&
    prev.isSelected === next.isSelected &&
    prev.showScoreLabels === next.showScoreLabels &&
    prev.hexStickyTooltips === next.hexStickyTooltips,
);
HexItem.displayName = "HexItem";

interface HexagonLayerProps {
  hexagons: HexagonDto[];
  selectedHexIndex?: string | null;
  showScoreLabels?: boolean;
  onHexClick?: (hex: HexagonDto) => void;
}

function HexagonLayer({
  hexagons,
  selectedHexIndex,
  showScoreLabels = true,
  onHexClick,
}: HexagonLayerProps) {
  const map = useMap();
  const [zoom, setZoom] = useState(() => map.getZoom());

  useEffect(() => {
    const onZoom = () => setZoom(map.getZoom());
    map.on("zoom zoomend", onZoom);
    return () => {
      map.off("zoom zoomend", onZoom);
    };
  }, [map]);

  const hexStickyTooltips = zoom >= 13;

  return (
    <>
      {hexagons.map((h) => (
        <HexItem
          key={h.h3Index}
          hex={h}
          isSelected={h.h3Index === selectedHexIndex}
          showScoreLabels={showScoreLabels}
          hexStickyTooltips={hexStickyTooltips}
          onHexClick={onHexClick}
        />
      ))}
    </>
  );
}

export default React.memo(HexagonLayer);
