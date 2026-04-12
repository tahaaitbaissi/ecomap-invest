"use client";

import { useCallback, useRef, useState } from "react";
import DynamicMap from "@/components/map/DynamicMap";
import type { BoundingBox, PoiDto } from "@/services/api/poiService";
import { fetchPoisInBbox } from "@/services/api/poiService";
import { searchAddress, type GeocodingResult } from "@/services/api/geocodingService";

export default function Map() {
  const [pois, setPois] = useState<PoiDto[]>([]);
  const [search, setSearch] = useState("");
  const [results, setResults] = useState<GeocodingResult[]>([]);
  const [flyTo, setFlyTo] = useState<{ lat: number; lng: number } | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleBoundsChange = useCallback(async (bbox: BoundingBox) => {
    try {
      const data = await fetchPoisInBbox(bbox);
      setPois(data);
    } catch {
      // backend may not be ready
    }
  }, []);

  const handleSearchChange = (value: string) => {
    setSearch(value);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (value.trim().length < 3) {
      setResults([]);
      return;
    }
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

  return (
    <div className="relative overflow-hidden rounded-2xl border border-slate-200 bg-white" style={{ minHeight: "500px" }}>
      <div className="absolute inset-0 z-0">
        <DynamicMap pois={pois} onBoundsChange={handleBoundsChange} flyTo={flyTo} />
      </div>

      <div className="pointer-events-none absolute left-1/2 top-4 z-[1000] w-[460px] max-w-[90%] -translate-x-1/2">
        <div
          className="pointer-events-auto flex flex-col rounded-2xl bg-white"
          style={{ boxShadow: "0 4px 24px rgba(0,0,0,0.25)" }}
        >
          <div className="flex items-center gap-2.5 rounded-full px-5 py-3">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="2">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              value={search}
              onChange={(e) => handleSearchChange(e.target.value)}
              placeholder="Search address... (e.g., Casablanca, Maarif, Anfa)"
              className="flex-1 border-none bg-transparent text-sm text-gray-700 outline-none placeholder:text-slate-400"
            />
          </div>
          {results.length > 0 && (
            <ul className="max-h-48 overflow-y-auto border-t border-slate-100 px-2 py-1">
              {results.map((r, i) => (
                <li key={i}>
                  <button
                    onClick={() => handleResultClick(r)}
                    className="w-full rounded-lg px-3 py-2 text-left text-xs text-slate-700 hover:bg-blue-50"
                  >
                    {r.displayName}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
