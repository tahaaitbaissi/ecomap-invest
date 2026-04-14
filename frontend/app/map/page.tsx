"use client";

import { useCallback, useState } from "react";
import DynamicMap from "@/components/map/DynamicMap";
import type { BoundingBox, PoiDto } from "@/services/api/poiService";
import { fetchPoisInBbox } from "@/services/api/poiService";

export default function MapPage() {
  const [pois, setPois] = useState<PoiDto[]>([]);

  const handleBoundsChange = useCallback(async (bbox: BoundingBox) => {
    try {
      const data = await fetchPoisInBbox(bbox);
      setPois(data);
    } catch (err) {
      console.debug("[MapPage] POI fetch skipped (backend not ready):", err);
    }
  }, []);

  return (
    <main className="relative h-screen w-screen overflow-hidden bg-gray-950">
      <header className="absolute left-0 right-0 top-0 z-[1000] flex items-center justify-between border-b border-gray-800 bg-gray-950/80 px-6 py-3 backdrop-blur-sm">
        <span className="text-sm font-semibold tracking-wide text-white">EcoMap Invest</span>
        <span className="text-xs text-gray-500">
          {pois.length > 0 ? `${pois.length} POIs in view` : "Casablanca Pilot - v0.1"}
        </span>
      </header>
      <div className="h-full w-full pt-12">
        <DynamicMap pois={pois} onBoundsChange={handleBoundsChange} />
      </div>
    </main>
  );
}
