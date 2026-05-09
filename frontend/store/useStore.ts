import { create } from "zustand";
import type { HexagonDto } from "@/services/api/hexagonService";
import type { DynamicProfileResponse } from "@/services/api/profileService";

/** Map fly-to target: optional bbox (south, west, north, east) for fitBounds, or targetZoom for flyTo. */
export type MapFlyToPayload = {
  lat: number;
  lng: number;
  label?: string;
  bbox?: [number, number, number, number];
  targetZoom?: number;
};

export interface GhostMarker {
  id: string;
  lat: number;
  lng: number;
  type: "competitor" | "driver";
  tag: string;
}

interface EcomapStore {
  activeView: string;
  setActiveView: (v: string) => void;
  mapFlyTo: MapFlyToPayload | null;
  setMapFlyTo: (p: MapFlyToPayload | null) => void;
  searchPin: { lat: number; lng: number; label?: string } | null;
  setSearchPin: (p: { lat: number; lng: number; label?: string } | null) => void;
  /** Ensures H3 selection is drawable when the cell is outside the current viewport fetch. */
  searchHighlightHex: HexagonDto | null;
  setSearchHighlightHex: (h: HexagonDto | null) => void;
  profileId: string | null;
  setProfileId: (id: string | null) => void;
  /** Preserve explicit neutral/manual selection across profile list refreshes. */
  profileSelectionMode: "auto" | "manual";
  commercialProfiles: DynamicProfileResponse[];
  setCommercialProfiles: (profiles: DynamicProfileResponse[]) => void;
  upsertCommercialProfile: (profile: DynamicProfileResponse) => void;
  removeCommercialProfile: (id: string) => void;
  selectedCommercialProfile: DynamicProfileResponse | null;
  hexagonRecord: Record<string, HexagonDto>;
  /** Replace viewport hex cells (avoids stale cells from previous bbox). */
  replaceHexagons: (hexs: HexagonDto[]) => void;
  selectedHexIndex: string | null;
  setSelectedHexIndex: (idx: string | null) => void;
  isSimulationActive: boolean;
  sessionId: string | null;
  ghostMarkers: GhostMarker[];
  startSimulation: () => void;
  stopSimulation: () => void;
  resetSimulation: () => void;
  addGhostMarker: (marker: Omit<GhostMarker, "id">) => void;
  showHeatmap: boolean;
  toggleHeatmap: () => void;
  showScoreLabels: boolean;
  toggleScoreLabels: () => void;
  showPoiMarkers: boolean;
  togglePoiMarkers: () => void;
  poiBusinessFilters: {
    drivers: boolean;
    competitors: boolean;
  };
  togglePoiBusinessFilter: (k: keyof EcomapStore["poiBusinessFilters"]) => void;
}

export const useStore = create<EcomapStore>((set) => ({
  activeView: "heatmap",
  setActiveView: (v) => set({ activeView: v }),
  mapFlyTo: null,
  setMapFlyTo: (p) => set({ mapFlyTo: p }),
  searchPin: null,
  setSearchPin: (p) => set({ searchPin: p }),
  searchHighlightHex: null,
  setSearchHighlightHex: (h) => set({ searchHighlightHex: h }),

  profileId: null,
  profileSelectionMode: "auto",
  setProfileId: (id) =>
    set((state) => ({
      profileId: id,
      profileSelectionMode: "manual",
      selectedCommercialProfile: state.commercialProfiles.find((p) => p.id === id) ?? null,
    })),
  commercialProfiles: [],
  setCommercialProfiles: (profiles) =>
    set((state) => {
      const currentValid = state.profileId ? profiles.find((p) => p.id === state.profileId) ?? null : null;
      const shouldAutoPick = state.profileSelectionMode === "auto" && !currentValid;
      const selected = currentValid ?? (shouldAutoPick ? profiles[0] ?? null : null);
      return {
        commercialProfiles: profiles,
        selectedCommercialProfile: selected,
        profileId: selected?.id ?? null,
      };
    }),
  upsertCommercialProfile: (profile) =>
    set((state) => {
      const rest = state.commercialProfiles.filter((p) => p.id !== profile.id);
      const profiles = [profile, ...rest];
      return {
        commercialProfiles: profiles,
        profileId: profile.id,
        selectedCommercialProfile: profile,
      };
    }),
  removeCommercialProfile: (id) =>
    set((state) => {
      const profiles = state.commercialProfiles.filter((p) => p.id !== id);
      const removedActive = state.profileId === id;
      const next =
        removedActive
          ? state.profileSelectionMode === "auto"
            ? profiles[0] ?? null
            : null
          : state.selectedCommercialProfile;
      return {
        commercialProfiles: profiles,
        profileId: removedActive ? next?.id ?? null : state.profileId,
        selectedCommercialProfile: next && profiles.some((p) => p.id === next.id) ? next : null,
      };
    }),
  selectedCommercialProfile: null,

  hexagonRecord: {},
  replaceHexagons: (hexs) =>
    set(() => {
      const next: Record<string, HexagonDto> = {};
      for (const h of hexs) {
        next[h.h3Index] = h;
      }
      return { hexagonRecord: next };
    }),

  selectedHexIndex: null,
  setSelectedHexIndex: (idx) => set({ selectedHexIndex: idx }),

  isSimulationActive: false,
  sessionId: null,
  ghostMarkers: [],
  startSimulation: () =>
    set({ isSimulationActive: true, sessionId: crypto.randomUUID(), ghostMarkers: [] }),
  stopSimulation: () => set({ isSimulationActive: false }),
  resetSimulation: () =>
    set({
      isSimulationActive: false,
      sessionId: null,
      ghostMarkers: [],
      searchPin: null,
      searchHighlightHex: null,
    }),
  addGhostMarker: (marker) =>
    set((state) => ({
      ghostMarkers: [...state.ghostMarkers, { ...marker, id: crypto.randomUUID() }],
    })),

  showHeatmap: true,
  toggleHeatmap: () => set((s) => ({ showHeatmap: !s.showHeatmap })),
  showScoreLabels: true,
  toggleScoreLabels: () => set((s) => ({ showScoreLabels: !s.showScoreLabels })),
  showPoiMarkers: true,
  togglePoiMarkers: () => set((s) => ({ showPoiMarkers: !s.showPoiMarkers })),
  poiBusinessFilters: {
    drivers: true,
    competitors: true,
  },
  togglePoiBusinessFilter: (k) =>
    set((s) => ({
      poiBusinessFilters: { ...s.poiBusinessFilters, [k]: !s.poiBusinessFilters[k] },
    })),
}));
