"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import DynamicMap from "@/components/map/DynamicMap";
import type { GhostMarkerDto, MapViewport } from "@/components/map/MapViewer";
import type { BoundingBox, PoiDto } from "@/services/api/poiService";
import { fetchPoisInBbox } from "@/services/api/poiService";
import { fetchHexagonsInBbox, type HexagonDto } from "@/services/api/hexagonService";
import { fetchMyProfilesAsList } from "@/services/api/profileService";
import {
  simulateOpportunity,
  type OpportunitySimulateResponse,
} from "@/services/api/simulationService";
import { useStore } from "@/store/useStore";
import { h3ResolutionForZoom } from "@/lib/mapHexResolution";
import MapHUD from "@/components/map/MapHUD";

export interface MapProps {
  simulationMode: boolean;
}

export default function Map({ simulationMode }: MapProps) {
  const replaceHexagons = useStore((s) => s.replaceHexagons);
  const setSelectedHexIndex = useStore((s) => s.setSelectedHexIndex);
  const selectedHexIndex = useStore((s) => s.selectedHexIndex);
  const setProfileIdStore = useStore((s) => s.setProfileId);
  const profileIdFromStore = useStore((s) => s.profileId);
  const profiles = useStore((s) => s.commercialProfiles);
  const setCommercialProfiles = useStore((s) => s.setCommercialProfiles);
  const selectedProfile = useStore((s) => s.selectedCommercialProfile);
  const showHeatmap = useStore((s) => s.showHeatmap);
  const showScoreLabels = useStore((s) => s.showScoreLabels);
  const showPoiMarkers = useStore((s) => s.showPoiMarkers);
  const poiBusinessFilters = useStore((s) => s.poiBusinessFilters);
  const mapFlyTo = useStore((s) => s.mapFlyTo);
  const setMapFlyTo = useStore((s) => s.setMapFlyTo);
  const searchPin = useStore((s) => s.searchPin);
  const setSearchPin = useStore((s) => s.setSearchPin);
  const searchHighlightHex = useStore((s) => s.searchHighlightHex);
  const setSearchHighlightHex = useStore((s) => s.setSearchHighlightHex);

  const [pois, setPois] = useState<PoiDto[]>([]);

  const [profilesResolved, setProfilesResolved] = useState(false);
  const [baselineHexagons, setBaselineHexagons] = useState<HexagonDto[]>([]);
  const [ghostMarkers, setGhostMarkers] = useState<GhostMarkerDto[]>([]);
  const [pendingClick, setPendingClick] = useState<{ lat: number; lng: number } | null>(null);
  const [opportunityOutcome, setOpportunityOutcome] = useState<OpportunitySimulateResponse | null>(null);
  const [explainWithLlm, setExplainWithLlm] = useState(false);
  const [simError, setSimError] = useState<string | null>(null);
  const [simLoading, setSimLoading] = useState(false);
  const [isLoadingHexagons, setIsLoadingHexagons] = useState(false);
  const [isLoadingPois, setIsLoadingPois] = useState(false);

  const lastBboxRef = useRef<BoundingBox | null>(null);
  const lastZoomRef = useRef<number>(13);
  const poiRequestSeqRef = useRef(0);
  const hexRequestSeqRef = useRef(0);
  const lastHexRequestKeyRef = useRef<string | null>(null);

  useEffect(() => {
    void fetchMyProfilesAsList(100)
      .then((list) => {
        setCommercialProfiles(list);
      })
      .catch(() => {
        /* not logged in or API down */
      })
      .finally(() => setProfilesResolved(true));
  }, [setCommercialProfiles]);

  useEffect(() => {
    if (profileIdFromStore) {
      void fetchMyProfilesAsList(100).then(setCommercialProfiles).catch(() => {});
    }
  }, [profileIdFromStore, setCommercialProfiles]);

  useEffect(() => {
    if (!simulationMode) {
      setGhostMarkers([]);
      setPendingClick(null);
      setOpportunityOutcome(null);
      setSimError(null);
      return;
    }
  }, [simulationMode]);

  useEffect(() => {
    setOpportunityOutcome(null);
  }, [profileIdFromStore]);

  useEffect(() => {
    replaceHexagons(baselineHexagons);
  }, [baselineHexagons, replaceHexagons]);

  const bboxKey = useCallback((bbox: BoundingBox) => {
    const { northEast: ne, southWest: sw } = bbox;
    return `${sw.lng.toFixed(5)},${sw.lat.toFixed(5)},${ne.lng.toFixed(5)},${ne.lat.toFixed(5)}`;
  }, []);

  const fetchHexagonsForViewport = useCallback(
    async (bbox: BoundingBox, zoom: number) => {
      if (!profilesResolved) return;
      const h3Res = h3ResolutionForZoom(zoom, simulationMode);
      const requestKey = `${bboxKey(bbox)}:${h3Res}:${profileIdFromStore ?? "none"}`;
      if (lastHexRequestKeyRef.current === requestKey) return;
      lastHexRequestKeyRef.current = requestKey;

      const seq = ++hexRequestSeqRef.current;
      const startedAt = performance.now();
      setIsLoadingHexagons(true);
      try {
        const hex = await fetchHexagonsInBbox(bbox, profileIdFromStore, h3Res);
        if (seq === hexRequestSeqRef.current) {
          setBaselineHexagons(hex);
          setOpportunityOutcome(null);
        }
        if (process.env.NODE_ENV !== "production") {
          console.debug("[Map] hex load", {
            count: hex.length,
            h3Resolution: h3Res,
            ms: Math.round(performance.now() - startedAt),
          });
        }
      } catch {
        if (seq === hexRequestSeqRef.current) {
          lastHexRequestKeyRef.current = null;
          setBaselineHexagons([]);
        }
      } finally {
        if (seq === hexRequestSeqRef.current) {
          setIsLoadingHexagons(false);
        }
      }
    },
    [bboxKey, profilesResolved, profileIdFromStore, simulationMode],
  );

  const handleBoundsChange = useCallback(
    (viewport: MapViewport) => {
      const { bbox, zoom } = viewport;
      lastBboxRef.current = bbox;
      lastZoomRef.current = zoom;

      const poiSeq = ++poiRequestSeqRef.current;
      const poiStartedAt = performance.now();
      setIsLoadingPois(true);
      void fetchPoisInBbox(bbox, { includeScore: false })
        .then((data) => {
          if (poiSeq === poiRequestSeqRef.current) {
            setPois(data);
          }
          if (process.env.NODE_ENV !== "production") {
            console.debug("[Map] POI load", {
              count: data.length,
              ms: Math.round(performance.now() - poiStartedAt),
            });
          }
        })
        .catch(() => {
          if (poiSeq === poiRequestSeqRef.current) {
            setPois([]);
          }
        })
        .finally(() => {
          if (poiSeq === poiRequestSeqRef.current) {
            setIsLoadingPois(false);
          }
        });

      void fetchHexagonsForViewport(bbox, zoom);
    },
    [fetchHexagonsForViewport],
  );

  useEffect(() => {
    const b = lastBboxRef.current;
    if (!b) return;
    void fetchHexagonsForViewport(b, lastZoomRef.current);
  }, [fetchHexagonsForViewport]);

  const mergedHexagons = useMemo(() => {
    if (!searchHighlightHex) return baselineHexagons;
    const i = baselineHexagons.findIndex((h) => h.h3Index === searchHighlightHex.h3Index);
    if (i >= 0) {
      const next = [...baselineHexagons];
      next[i] = searchHighlightHex;
      return next;
    }
    return [...baselineHexagons, searchHighlightHex];
  }, [baselineHexagons, searchHighlightHex]);

  // Per spec: clicking the map clears the transient search pin, but should not
  // wipe hex selection/highlight (otherwise hex interactions feel “broken”).
  const clearSearchPinOnMapClick = useCallback(() => {
    setSearchPin(null);
  }, [setSearchPin]);

  const filteredPois = (() => {
    if (!showPoiMarkers) return [];

    // If no profile is active, keep showing all POIs (so the map isn't empty).
    if (!selectedProfile) return pois;

    const driverTags = new Set(selectedProfile.drivers.map((d) => d.tag));
    const competitorTags = new Set(selectedProfile.competitors.map((c) => c.tag));

    const wantDrivers = poiBusinessFilters.drivers;
    const wantCompetitors = poiBusinessFilters.competitors;

    // If both toggles are off, show none (explicit intent).
    if (!wantDrivers && !wantCompetitors) return [];

    return pois.filter((p) => {
      const tag = p.typeTag ?? "";
      const isDriver = driverTags.has(tag);
      const isCompetitor = competitorTags.has(tag);
      if (wantDrivers && isDriver) return true;
      if (wantCompetitors && isCompetitor) return true;
      return false;
    });
  })();

  useEffect(() => {
    if (!mapFlyTo) return;
    // The map consumes flyTo and should not repeat it on re-renders.
    const t = setTimeout(() => setMapFlyTo(null), 700);
    return () => clearTimeout(t);
  }, [mapFlyTo, setMapFlyTo]);

  const onSimulationMapClick = useCallback(
    (lat: number, lng: number) => {
      if (!profileIdFromStore || !selectedProfile) return;
      setPendingClick({ lat, lng });
      setOpportunityOutcome(null);
      setSimError(null);
    },
    [profileIdFromStore, selectedProfile],
  );

  const handleConfirmSimulation = async () => {
    if (!pendingClick || !profileIdFromStore) return;
    setSimLoading(true);
    setSimError(null);
    try {
      const res = await simulateOpportunity({
        lat: pendingClick.lat,
        lng: pendingClick.lng,
        profileId: profileIdFromStore,
        explain: explainWithLlm,
      });
      const id = crypto.randomUUID();
      setGhostMarkers((g) => [
        ...g,
        {
          id,
          lat: pendingClick.lat,
          lng: pendingClick.lng,
          type: "OPPORTUNITY",
          tag: res.archetypeId,
        },
      ]);
      setOpportunityOutcome(res);
      setPendingClick(null);
    } catch (e) {
      setSimError(e instanceof Error ? e.message : "Simulation failed");
    } finally {
      setSimLoading(false);
    }
  };

  const handleResetSimulation = () => {
    setGhostMarkers([]);
    setPendingClick(null);
    setOpportunityOutcome(null);
    setSimError(null);
  };

  const closeSimPanel = () => {
    setPendingClick(null);
    setOpportunityOutcome(null);
    setSimError(null);
  };

  return (
    <div
      className="relative overflow-hidden rounded-2xl border border-slate-200 bg-white"
      style={{ minHeight: "500px", flex: 1 }}
    >
      <div className="absolute inset-0 z-0">
        <DynamicMap
          pois={filteredPois}
          hexagons={mergedHexagons}
          selectedHexIndex={selectedHexIndex}
          ghostMarkers={ghostMarkers}
          showHeatmap={showHeatmap}
          showScoreLabels={showScoreLabels}
          showPoiMarkers={showPoiMarkers}
          onBoundsChange={handleBoundsChange}
          onHexClick={(h) => setSelectedHexIndex(h.h3Index)}
          flyTo={mapFlyTo}
          searchPin={searchPin}
          onDismissSearchPin={() => setSearchPin(null)}
          onMapBackgroundClick={clearSearchPinOnMapClick}
          simulationMode={simulationMode}
          onSimulationMapClick={simulationMode ? onSimulationMapClick : undefined}
        />
      </div>

      <MapHUD
        topRight={
          <>
            {isLoadingHexagons ? (
              <div className="pointer-events-none flex items-center gap-2 rounded-full border border-slate-200/80 bg-white/90 px-3 py-1.5 text-xs font-medium text-slate-600 shadow-md backdrop-blur-sm">
                <span className="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-slate-300 border-t-blue-600" />
                Chargement des zones…
              </div>
            ) : null}
            {isLoadingPois ? (
              <div className="pointer-events-none mt-2 flex items-center gap-2 rounded-full border border-slate-200/80 bg-white/90 px-3 py-1.5 text-xs font-medium text-slate-600 shadow-md backdrop-blur-sm">
                <span className="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-slate-300 border-t-blue-600" />
                Chargement des POI…
              </div>
            ) : null}
          </>
        }
        bottomLeft={
          simulationMode ? (
            <div className="pointer-events-auto max-w-full rounded-xl border border-slate-200 bg-white/95 px-4 py-3 text-xs shadow-lg backdrop-blur-sm">
              <p className="mb-1 font-semibold text-slate-800">What-if</p>
              {profiles.length > 0 && (
                <label className="mb-2 flex flex-col gap-1 text-slate-600">
                  Profile
                  <select
                    className="rounded border border-slate-200 px-2 py-1 text-slate-800"
                    value={profileIdFromStore ?? ""}
                    onChange={(e) => setProfileIdStore(e.target.value || null)}
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
              {!profileIdFromStore ? (
                <p className="text-amber-700">Generate a dynamic profile to run simulations.</p>
              ) : (
                <p className="text-slate-500">
                  Cliquez sur la carte pour simuler l’ouverture de ce profil à cet emplacement (score d’opportunité).
                </p>
              )}
              <button
                type="button"
                className="mt-2 rounded-lg border border-slate-200 px-3 py-1.5 text-slate-700 hover:bg-slate-50"
                onClick={handleResetSimulation}
              >
                Réinitialiser
              </button>
            </div>
          ) : null
        }
      >
        {simulationMode && profileIdFromStore && (pendingClick || opportunityOutcome) ? (
          <div className="pointer-events-none absolute right-4 top-4 w-[min(360px,92vw)]">
            <div
              className="pointer-events-auto max-h-[min(70vh,560px)] overflow-y-auto rounded-2xl border border-slate-200 bg-white p-4 shadow-xl"
              style={{ boxShadow: "0 4px 24px rgba(0,0,0,0.2)" }}
            >
              {pendingClick ? (
                <>
                  <p className="mb-1 text-sm font-semibold text-slate-800">Simuler l&apos;ouverture</p>
                  <p className="mb-1 text-xs font-medium text-slate-700">{selectedProfile?.userQuery}</p>
                  <p className="mb-3 text-xs text-slate-500">
                    {pendingClick.lat.toFixed(5)}, {pendingClick.lng.toFixed(5)}
                  </p>
                  <label className="mb-3 flex cursor-pointer items-center gap-2 text-xs text-slate-600">
                    <input
                      type="checkbox"
                      checked={explainWithLlm}
                      onChange={(e) => setExplainWithLlm(e.target.checked)}
                    />
                    Explication (LLM optionnelle — les scores restent déterministes)
                  </label>
                  {simError ? <p className="mb-2 text-xs text-red-600">{simError}</p> : null}
                  <div className="flex gap-2">
                    <button
                      type="button"
                      disabled={simLoading}
                      className="flex-1 rounded-lg bg-blue-600 px-3 py-2 text-xs font-medium text-white disabled:opacity-50"
                      onClick={() => void handleConfirmSimulation()}
                    >
                      {simLoading ? "…" : "Calculer"}
                    </button>
                    <button
                      type="button"
                      className="rounded-lg border border-slate-200 px-3 py-2 text-xs text-slate-700"
                      onClick={closeSimPanel}
                    >
                      Fermer
                    </button>
                  </div>
                </>
              ) : opportunityOutcome ? (
                <>
                  <p className="mb-2 text-sm font-semibold text-slate-800">Score d&apos;opportunité</p>
                  <p className="mb-3 text-2xl font-bold text-blue-700">
                    {Math.round(opportunityOutcome.opportunityScore)}{" "}
                    <span className="text-sm font-semibold text-slate-500">/ 100</span>
                  </p>
                  <ul className="mb-3 space-y-1 border-t border-slate-100 pt-2 text-xs text-slate-600">
                    <li>Demande (drivers + démo) : {opportunityOutcome.demandScore.toFixed(1)}</li>
                    <li>Pénalité concurrence : {opportunityOutcome.competitionPenalty.toFixed(1)}</li>
                    <li>Effet clustering : +{opportunityOutcome.clusterEffectBonus.toFixed(1)}</li>
                    <li>
                      Adéquation lieux ({opportunityOutcome.archetypeId}) : +{opportunityOutcome.businessFitBonus.toFixed(1)}
                    </li>
                    <li className="text-slate-500">
                      Concurrence profil (~500 m) : {opportunityOutcome.competitorCountNearby} POI
                    </li>
                    <li className="text-slate-500">
                      Concurrence (même cellule H3 que la carte) : {opportunityOutcome.competitorCountInHex} POI
                    </li>
                    <li className="font-mono text-[10px] text-slate-400">H3 {opportunityOutcome.h3Index.slice(-12)}</li>
                  </ul>
                  {opportunityOutcome.explanation ? (
                    <p className="mb-3 rounded-lg bg-slate-50 px-3 py-2 text-xs leading-relaxed text-slate-700">
                      {opportunityOutcome.explanation}
                    </p>
                  ) : null}
                  <button
                    type="button"
                    className="w-full rounded-lg border border-slate-200 px-3 py-2 text-xs text-slate-700 hover:bg-slate-50"
                    onClick={closeSimPanel}
                  >
                    Fermer
                  </button>
                </>
              ) : null}
            </div>
          </div>
        ) : null}
      </MapHUD>

    </div>
  );
}
