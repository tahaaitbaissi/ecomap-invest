"use client";

import { useEffect, useRef, useState } from "react";

type LeafletMap = import("leaflet").Map;

export default function Map() {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const leafletRef = useRef<LeafletMap | null>(null);
  const [search, setSearch] = useState("");

  useEffect(() => {
    if (!mapContainerRef.current || leafletRef.current) return;

    import("leaflet").then((L) => {
      const map = L.map(mapContainerRef.current!, {
        center: [30.4, -8.5],
        zoom: 8,
        zoomControl: false,
        attributionControl: false,
      });

      L.tileLayer("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}", { maxZoom: 19 }).addTo(map);

      L.tileLayer("https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}", {
        opacity: 0.8,
        maxZoom: 19,
      }).addTo(map);

      L.control.zoom({ position: "bottomright" }).addTo(map);

      leafletRef.current = map;
    });

    return () => {
      if (leafletRef.current) {
        leafletRef.current.remove();
        leafletRef.current = null;
      }
    };
  }, []);

  return (
    <div style={{ flex: 1, position: "relative", overflow: "hidden" }}>
      <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />

      <div ref={mapContainerRef} style={{ position: "absolute", inset: 0, zIndex: 0 }} />

      <div style={{ position: "absolute", top: "16px", left: "50%", transform: "translateX(-50%)", zIndex: 30, width: "460px" }}>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "10px",
            background: "#fff",
            borderRadius: "999px",
            padding: "11px 20px",
            boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
          }}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="2">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search address... (e.g., Casablanca, Maarif, Anfa)"
            style={{ border: "none", outline: "none", flex: 1, fontSize: "14px", color: "#374151", background: "transparent" }}
          />
        </div>
      </div>
    </div>
  );
}
