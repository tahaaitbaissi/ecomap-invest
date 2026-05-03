"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import DynamicMap from "@/components/map/DynamicMap";
import type { GhostMarkerDto } from "@/components/map/MapViewer";
import type { BoundingBox, PoiDto } from "@/services/api/poiService";
import { fetchPoisInBbox } from "@/services/api/poiService";
import { searchAddress, type GeocodingResult } from "@/services/api/geocodingService";
import { fetchHexagonsInBbox, type HexagonDto } from "@/services/api/hexagonService";
import { fetchMyProfilesAsList, type DynamicProfileResponse } from "@/services/api/profileService";
import {
  deleteSimulationSession,
  simulateImpact,
  type SimulationImpactType,
} from "@/services/api/simulationService";

export interface MapProps {
  simulationMode: boolean;
}

export default function Map({ simulationMode }: MapProps) {
  const [pois, setPois] = useState<PoiDto[]>([]);
  const [search, setSearch] = useState("");
  const [results, setResults] = useState<GeocodingResult[]>([]);
  const [flyTo, setFlyTo] = useState<{ lat: number; lng: number } | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [profiles, setProfiles] = useState<DynamicProfileResponse[]>([]);
  const [selectedProfileId, setSelectedProfileId] = useState<string | null>(null);
  const [baselineHexagons, setBaselineHexagons] = useState<HexagonDto[]>([]);
  const [simScoreOverrides, setSimScoreOverrides] = useState<Record<string, number>>({});
  const [ghostMarkers, setGhostMarkers] = useState<GhostMarkerDto[]>([]);
  const [pendingClick, setPendingClick] = useState<{ lat: number; lng: number } | null>(null);
  const [simType, setSimType] = useState<SimulationImpactType>("DRIVER");
  const [simTag, setSimTag] = useState("");
  const [simError, setSimError] = useState<string | null>(null);
  const [simLoading, setSimLoading] = useState(false);

  const sessionIdRef = useRef<string | null>(null);
  const lastBboxRef = useRef<BoundingBox | null>(null);

  useEffect(() => {
    void fetchMyProfilesAsList(100)
      .then((list) => {
        setProfiles(list);
        if (list.length > 0) {
          setSelectedProfileId(list[0].id);
        }
      })
      .catch(() => {
        /* not logged in or API down */
      });
  }, []);

  useEffect(() => {
    if (!simulationMode) {
      const sid = sessionIdRef.current;
      if (sid) {
        void deleteSimulationSession(sid).catch(() => {});
      }
      sessionIdRef.current = null;
      setSimScoreOverrides({});
      setGhostMarkers([]);
      setPendingClick(null);
      setSimError(null);
      return;
    }
    sessionIdRef.current = crypto.randomUUID();
  }, [simulationMode]);

  useEffect(() => {
    setSimScoreOverrides({});
  }, [selectedProfileId]);

  const selectedProfile = useMemo(
    () => profiles.find((p) => p.id === selectedProfileId) ?? null,
    [profiles, selectedProfileId],
  );

  const tagOptions = useMemo(() => {
    if (!selectedProfile) return [];
    return simType === "DRIVER" ? selectedProfile.drivers : selectedProfile.competitors;
  }, [selectedProfile, simType]);

  useEffect(() => {
    const first = tagOptions[0]?.tag ?? "";
    setSimTag(first);
  }, [simType, tagOptions]);

  const displayHexagons = useMemo(() => {
    if (Object.keys(simScoreOverrides).length === 0) return baselineHexagons;
    return baselineHexagons.map((h) => {
      const s = simScoreOverrides[h.h3Index];
      return s !== undefined ? { ...h, score: s } : h;
    });
  }, [baselineHexagons, simScoreOverrides]);

  const handleBoundsChange = useCallback(
    async (bbox: BoundingBox) => {
      lastBboxRef.current = bbox;
      try {
        const poiData = await fetchPoisInBbox(bbox);
        setPois(poiData);
      } catch {
        /* backend may not be ready */
      }
      try {
        const hexData = await fetchHexagonsInBbox(bbox, selectedProfileId);
        setBaselineHexagons(hexData);
      } catch {
        setBaselineHexagons([]);
      }
    },
    [selectedProfileId],
  );

  useEffect(() => {
    const b = lastBboxRef.current;
    if (!b) return;
    void fetchHexagonsInBbox(b, selectedProfileId)
      .then(setBaselineHexagons)
      .catch(() => setBaselineHexagons([]));
  }, [selectedProfileId]);

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

  const onSimulationMapClick = useCallback((lat: number, lng: number) => {
    if (!selectedProfileId || !selectedProfile) return;
    setPendingClick({ lat, lng });
    setSimError(null);
  }, [selectedProfileId, selectedProfile]);

  const handleConfirmSimulation = async () => {
    if (!pendingClick || !selectedProfileId || !sessionIdRef.current) return;
    if (!simTag.trim()) {
      setSimError("Choose a tag");
      return;
    }
    setSimLoading(true);
    setSimError(null);
    try {
      const res = await simulateImpact({
        lat: pendingClick.lat,
        lng: pendingClick.lng,
        type: simType,
        tag: simTag.trim(),
        profileId: selectedProfileId,
        sessionId: sessionIdRef.current,
      });
      setSimScoreOverrides((prev) => {
        const next = { ...prev };
        for (const h of res.affectedHexagons) {
          if (h.score != null) next[h.h3Index] = h.score;
        }
        return next;
      });
      const id = crypto.randomUUID();
      setGhostMarkers((g) => [
        ...g,
        {
          id,
          lat: pendingClick.lat,
          lng: pendingClick.lng,
          type: simType,
          tag: simTag.trim(),
        },
      ]);
      setPendingClick(null);
    } catch (e) {
      setSimError(e instanceof Error ? e.message : "Simulation failed");
    } finally {
      setSimLoading(false);
    }
  };

  const handleResetSimulation = () => {
    const sid = sessionIdRef.current;
    if (sid) {
      void deleteSimulationSession(sid).catch(() => {});
    }
    sessionIdRef.current = simulationMode ? crypto.randomUUID() : null;
    setSimScoreOverrides({});
    setGhostMarkers([]);
    setPendingClick(null);
    setSimError(null);
  };

  return (
    <div className="relative overflow-hidden rounded-2xl border border-slate-200 bg-white" style={{ minHeight: "500px" }}>
      <div className="absolute inset-0 z-0">
        <DynamicMap
          pois={pois}
          hexagons={displayHexagons}
          onBoundsChange={handleBoundsChange}
          flyTo={flyTo}
          simulationMode={simulationMode}
          onSimulationMapClick={simulationMode ? onSimulationMapClick : undefined}
          ghostMarkers={ghostMarkers}
        />
      </div>

      {simulationMode && (
        <div className="pointer-events-none absolute bottom-4 left-4 right-4 z-[1000] flex flex-wrap items-end justify-between gap-3">
          <div
            className="pointer-events-auto max-w-full rounded-xl border border-slate-200 bg-white/95 px-4 py-3 text-xs shadow-lg backdrop-blur-sm"
            style={{ minWidth: "200px" }}
          >
            <p className="mb-1 font-semibold text-slate-800">What-if</p>
            {profiles.length > 1 && (
              <label className="mb-2 flex flex-col gap-1 text-slate-600">
                Profile
                <select
                  className="rounded border border-slate-200 px-2 py-1 text-slate-800"
                  value={selectedProfileId ?? ""}
                  onChange={(e) => setSelectedProfileId(e.target.value || null)}
                >
                  {profiles.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.userQuery.slice(0, 40)}
                      {p.userQuery.length > 40 ? "…" : ""}
                    </option>
                  ))}
                </select>
              </label>
            )}
            {!selectedProfileId && <p className="text-amber-700">Generate a dynamic profile to run simulations.</p>}
            {selectedProfileId && (
              <p className="text-slate-500">Click the map to place a ghost POI (crosshair cursor).</p>
            )}
            <button
              type="button"
              className="mt-2 rounded-lg border border-slate-200 px-3 py-1.5 text-slate-700 hover:bg-slate-50"
              onClick={handleResetSimulation}
            >
              Reset ring scores
            </button>
          </div>
        </div>
      )}

      {simulationMode && pendingClick && selectedProfileId && (
        <div className="pointer-events-none absolute right-4 top-24 z-[1001] w-[300px] max-w-[90vw]">
          <div
            className="pointer-events-auto rounded-2xl border border-slate-200 bg-white p-4 shadow-xl"
            style={{ boxShadow: "0 4px 24px rgba(0,0,0,0.2)" }}
          >
            <p className="mb-2 text-sm font-semibold text-slate-800">Ghost POI</p>
            <p className="mb-3 text-xs text-slate-500">
              {pendingClick.lat.toFixed(5)}, {pendingClick.lng.toFixed(5)}
            </p>
            <label className="mb-2 flex flex-col gap-1 text-xs text-slate-600">
              Type
              <select
                className="rounded-lg border border-slate-200 px-2 py-1.5 text-slate-800"
                value={simType}
                onChange={(e) => setSimType(e.target.value as SimulationImpactType)}
              >
                <option value="DRIVER">Driver</option>
                <option value="COMPETITOR">Competitor</option>
              </select>
            </label>
            <label className="mb-3 flex flex-col gap-1 text-xs text-slate-600">
              Tag
              <select
                className="rounded-lg border border-slate-200 px-2 py-1.5 text-slate-800"
                value={simTag}
                onChange={(e) => setSimTag(e.target.value)}
              >
                {tagOptions.map((t) => (
                  <option key={t.tag} value={t.tag}>
                    {t.tag} ({t.weight})
                  </option>
                ))}
              </select>
            </label>
            {tagOptions.length === 0 && (
              <p className="mb-2 text-xs text-amber-700">No tags for this type in the profile.</p>
            )}
            {simError && <p className="mb-2 text-xs text-red-600">{simError}</p>}
            <div className="flex gap-2">
              <button
                type="button"
                disabled={simLoading || tagOptions.length === 0}
                className="flex-1 rounded-lg bg-blue-600 px-3 py-2 text-xs font-medium text-white disabled:opacity-50"
                onClick={() => void handleConfirmSimulation()}
              >
                {simLoading ? "…" : "Apply"}
              </button>
              <button
                type="button"
                className="rounded-lg border border-slate-200 px-3 py-2 text-xs text-slate-700"
                onClick={() => setPendingClick(null)}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

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
