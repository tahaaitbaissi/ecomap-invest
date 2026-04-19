"use client";

import React, { useMemo } from "react";
import { Polygon, Tooltip, Marker } from "react-leaflet";
import L from "leaflet";
import type { HexagonDto } from "@/services/api/hexagonService";
import { getHexColor, getHexOpacity } from "@/lib/hexagonUtils";

interface HexItemProps {
  hex: HexagonDto;
  isSelected: boolean;
  showScoreLabels: boolean;
  onHexClick?: (h: HexagonDto) => void;
}

const HexItem = React.memo(
  ({ hex, isSelected, showScoreLabels, onHexClick }: HexItemProps) => {
    const color = getHexColor(hex.score);
    const opacity = getHexOpacity(hex.score);

    const positions = useMemo(
      () => hex.boundary.map((p) => [p.lat, p.lng] as [number, number]),
      // eslint-disable-next-line react-hooks/exhaustive-deps
      [hex.h3Index]
    );

    const center = useMemo(() => {
      const lat = hex.boundary.reduce((s, p) => s + p.lat, 0) / hex.boundary.length;
      const lng = hex.boundary.reduce((s, p) => s + p.lng, 0) / hex.boundary.length;
      return [lat, lng] as [number, number];
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [hex.h3Index]);

    const scoreIcon = useMemo(
      () =>
        L.divIcon({
          html: `<span style="font-size:11px;font-weight:700;color:#fff;text-shadow:0 1px 3px rgba(0,0,0,0.7);pointer-events:none;user-select:none;white-space:nowrap">${hex.score}</span>`,
          className: "",
          iconSize: [28, 16],
          iconAnchor: [14, 8],
        }),
      [hex.score]
    );

    return (
      <>
        <Polygon
          positions={positions}
          pathOptions={{
            fillColor: color,
            color: isSelected ? "#ffffff" : "#555",
            fillOpacity: isSelected ? Math.min(opacity + 0.2, 0.95) : opacity,
            weight: isSelected ? 3 : 0.8,
          }}
          eventHandlers={{ click: () => onHexClick?.(hex) }}
        >
          <Tooltip sticky>
            <span style={{ fontWeight: 600 }}>Score : {hex.score}</span>
            <br />
            <span style={{ fontSize: "10px", color: "#666" }}>{hex.h3Index.slice(-8)}</span>
          </Tooltip>
        </Polygon>
        {showScoreLabels && (
          <Marker
            position={center}
            icon={scoreIcon}
            interactive={false}
            eventHandlers={{ click: () => onHexClick?.(hex) }}
          />
        )}
      </>
    );
  },
  (prev, next) =>
    prev.hex.h3Index === next.hex.h3Index &&
    prev.hex.score === next.hex.score &&
    prev.isSelected === next.isSelected &&
    prev.showScoreLabels === next.showScoreLabels
);
HexItem.displayName = "HexItem";

interface HexagonLayerProps {
  hexagons: HexagonDto[];
  selectedHexIndex?: string | null;
  showScoreLabels?: boolean;
  onHexClick?: (hex: HexagonDto) => void;
}

function HexagonLayer({ hexagons, selectedHexIndex, showScoreLabels = true, onHexClick }: HexagonLayerProps) {
  return (
    <>
      {hexagons.map((h) => (
        <HexItem
          key={h.h3Index}
          hex={h}
          isSelected={h.h3Index === selectedHexIndex}
          showScoreLabels={showScoreLabels}
          onHexClick={onHexClick}
        />
      ))}
    </>
  );
}

export default React.memo(HexagonLayer);
