"use client";

import { useEffect, useRef } from "react";
import { useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet.markercluster";
import "leaflet.markercluster/dist/MarkerCluster.css";
import "leaflet.markercluster/dist/MarkerCluster.Default.css";
import type { PoiDto } from "@/services/api/poiService";

type MarkerClusterGroup = L.LayerGroup & {
  addLayer(layer: L.Marker): MarkerClusterGroup;
  clearLayers(): MarkerClusterGroup;
};

function createClusterGroup(): MarkerClusterGroup {
  const factory = (L as unknown as { markerClusterGroup: (opts?: Record<string, unknown>) => MarkerClusterGroup })
    .markerClusterGroup;
  return factory({
    chunkedLoading: true,
    maxClusterRadius: 72,
    spiderfyOnMaxZoom: true,
    showCoverageOnHover: false,
    zoomToBoundsOnClick: true,
    disableClusteringAtZoom: 16,
  });
}

function popupHtml(poi: PoiDto): string {
  const name = poi.name ?? "Unknown";
  const tag = poi.typeTag ?? "";
  const addr = poi.address ? `<br/><span style="font-size:11px">${escapeHtml(poi.address)}</span>` : "";
  return `<strong>${escapeHtml(name)}</strong><br/><span style="font-size:11px;color:#64748b">${escapeHtml(tag)}</span>${addr}`;
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

export default function PoiClusterLayer({
  pois,
  enabled = true,
}: {
  pois?: PoiDto[];
  enabled?: boolean;
}) {
  const map = useMap();
  const groupRef = useRef<MarkerClusterGroup | null>(null);

  useEffect(() => {
    if (!enabled) {
      if (groupRef.current) {
        map.removeLayer(groupRef.current);
        groupRef.current = null;
      }
      return;
    }

    const group = createClusterGroup();
    groupRef.current = group;
    map.addLayer(group);

    for (const poi of pois ?? []) {
      if (poi.latitude == null || poi.longitude == null) continue;
      const m = L.marker([poi.latitude, poi.longitude]);
      m.bindPopup(popupHtml(poi));
      group.addLayer(m);
    }

    return () => {
      map.removeLayer(group);
      groupRef.current = null;
    };
  }, [map, enabled, pois]);

  return null;
}
