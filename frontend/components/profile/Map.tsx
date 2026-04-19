"use client";

import { useCallback, useRef, useState } from "react";
import DynamicMap from "@/components/map/DynamicMap";
import type { BoundingBox } from "@/services/api/poiService";
import { fetchPoisInBbox, type PoiDto } from "@/services/api/poiService";
import { fetchHexagonsInBbox, type HexagonDto } from "@/services/api/hexagonService";
import { searchAddress, type GeocodingResult } from "@/services/api/geocodingService";
import { useStore } from "@/store/useStore";

export default function Map() {
  const {
    hexagonRecord,
    mergeHexagons,
    selectedHexIndex,
    setSelectedHexIndex,
    isSimulationActive,
    ghostMarkers,
    addGhostMarker,
    showHeatmap,
    showScoreLabels,
    showPoiMarkers,
  } = useStore();

  const hexagons = Object.values(hexagonRecord);

  const [pois, setPois] = useState<PoiDto[]>([]);
  const [search, setSearch] = useState("");
  const [results, setResults] = useState<GeocodingResult[]>([]);
  const [flyTo, setFlyTo] = useState<{ lat: number; lng: number } | null>(null);
  const [pendingSimPos, setPendingSimPos] = useState<{ lat: number; lng: number } | null>(null);
  const [simType, setSimType] = useState<"competitor" | "driver">("driver");
  const [simTag, setSimTag] = useState("");
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleBoundsChange = useCallback(
    async (bbox: BoundingBox) => {
      try {
        const [poisData, hexData] = await Promise.all([
          fetchPoisInBbox(bbox),
          fetchHexagonsInBbox(bbox),
        ]);
        setPois(poisData);
        mergeHexagons(hexData);
      } catch {
        // backend may not be ready
      }
    },
    [mergeHexagons]
  );

  const handleSearchChange = (value: string) => {
    setSearch(value);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (value.trim().length < 3) { setResults([]); return; }
    debounceRef.current = setTimeout(async () => {
      try {
        const res = await searchAddress(value.trim());
        setResults(res);
      } catch {
        setResults([]);
      }
    }, 400);
  };

  const handleResultClick = (result: GeocodingResult) => {
    setFlyTo({ lat: result.lat, lng: result.lng });
    setSearch(result.displayName);
    setResults([]);
    setTimeout(() => setFlyTo(null), 500);
  };

  const handleMapClick = (lat: number, lng: number) => {
    if (isSimulationActive) setPendingSimPos({ lat, lng });
  };

  const confirmMarker = () => {
    if (!pendingSimPos || !simTag.trim()) return;
    addGhostMarker({ lat: pendingSimPos.lat, lng: pendingSimPos.lng, type: simType, tag: simTag.trim() });
    setPendingSimPos(null);
    setSimTag("");
  };

  return (
    <div className="relative overflow-hidden rounded-2xl border border-slate-200 bg-white" style={{ minHeight: "500px", flex: 1 }}>
      <div className="absolute inset-0 z-0">
        <DynamicMap
          pois={pois}
          hexagons={hexagons}
          selectedHexIndex={selectedHexIndex}
          ghostMarkers={ghostMarkers}
          isSimulationActive={isSimulationActive}
          showHeatmap={showHeatmap}
          showScoreLabels={showScoreLabels}
          showPoiMarkers={showPoiMarkers}
          onBoundsChange={handleBoundsChange}
          onHexClick={(h: HexagonDto) => setSelectedHexIndex(h.h3Index)}
          onMapClick={handleMapClick}
          flyTo={flyTo}
        />
      </div>

      {/* Search bar */}
      <div className="pointer-events-none absolute left-1/2 top-4 z-[1000] w-[460px] max-w-[90%] -translate-x-1/2">
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
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder="Search address... (e.g., Casablanca, Maarif, Anfa)"
            style={{ border: "none", outline: "none", flex: 1, fontSize: "14px", color: "#374151", background: "transparent", pointerEvents: "auto" }}
          />
        </div>
        {results.length > 0 && (
          <ul style={{ marginTop: "6px", background: "#fff", borderRadius: "12px", boxShadow: "0 4px 24px rgba(0,0,0,0.15)", overflow: "hidden", pointerEvents: "auto" }}>
            {results.map((r, i) => (
              <li
                key={i}
                onClick={() => handleResultClick(r)}
                style={{ padding: "9px 20px", fontSize: "13px", color: "#374151", cursor: "pointer", borderBottom: i < results.length - 1 ? "1px solid #f1f5f9" : "none" }}
              >
                {r.displayName}
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Simulation mode hint */}
      {isSimulationActive && !pendingSimPos && (
        <div
          style={{ position: "absolute", bottom: "24px", left: "50%", transform: "translateX(-50%)", zIndex: 1000, background: "rgba(20,20,30,0.75)", color: "#fff", fontSize: "12px", padding: "6px 16px", borderRadius: "20px", backdropFilter: "blur(8px)", whiteSpace: "nowrap", pointerEvents: "none" }}
        >
          Cliquez sur la carte pour placer un marqueur
        </div>
      )}

      {/* Simulation confirm panel */}
      {pendingSimPos && (
        <div
          style={{ position: "absolute", bottom: "24px", left: "50%", transform: "translateX(-50%)", zIndex: 1000, background: "rgba(255,255,255,0.96)", backdropFilter: "blur(12px)", borderRadius: "16px", border: "1px solid #e2e8f0", padding: "14px 16px", boxShadow: "0 8px 32px rgba(0,0,0,0.15)", minWidth: "260px" }}
        >
          <p style={{ fontSize: "12px", fontWeight: 700, color: "#1e293b", marginBottom: "10px" }}>Ajouter un marqueur</p>
          <div style={{ display: "flex", gap: "6px", marginBottom: "10px" }}>
            <button
              onClick={() => setSimType("driver")}
              style={{ flex: 1, padding: "6px", borderRadius: "8px", border: `1.5px solid ${simType === "driver" ? "#22c55e" : "#e2e8f0"}`, background: simType === "driver" ? "#f0fdf4" : "#fff", color: simType === "driver" ? "#166534" : "#374151", fontSize: "11px", fontWeight: 600, cursor: "pointer" }}
            >
              + Driver
            </button>
            <button
              onClick={() => setSimType("competitor")}
              style={{ flex: 1, padding: "6px", borderRadius: "8px", border: `1.5px solid ${simType === "competitor" ? "#ef4444" : "#e2e8f0"}`, background: simType === "competitor" ? "#fef2f2" : "#fff", color: simType === "competitor" ? "#991b1b" : "#374151", fontSize: "11px", fontWeight: 600, cursor: "pointer" }}
            >
              ✕ Concurrent
            </button>
          </div>
          <input
            value={simTag}
            onChange={(e) => setSimTag(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && confirmMarker()}
            placeholder="Tag (ex: boulangerie)"
            style={{ width: "100%", padding: "7px 10px", borderRadius: "8px", border: "1.5px solid #e2e8f0", fontSize: "11px", outline: "none", boxSizing: "border-box", marginBottom: "10px" }}
          />
          <div style={{ display: "flex", gap: "6px" }}>
            <button
              onClick={() => setPendingSimPos(null)}
              style={{ flex: 1, padding: "7px", borderRadius: "8px", border: "1px solid #e2e8f0", background: "#fff", color: "#374151", fontSize: "11px", cursor: "pointer" }}
            >
              Annuler
            </button>
            <button
              onClick={confirmMarker}
              disabled={!simTag.trim()}
              style={{ flex: 1, padding: "7px", borderRadius: "8px", border: "none", background: !simTag.trim() ? "#94a3b8" : "#1a56db", color: "#fff", fontSize: "11px", fontWeight: 600, cursor: !simTag.trim() ? "not-allowed" : "pointer" }}
            >
              Ajouter
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
