import { create } from "zustand";
import type { HexagonDto } from "@/services/api/hexagonService";

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
  profileId: string | null;
  setProfileId: (id: string | null) => void;
  hexagonRecord: Record<string, HexagonDto>;
  mergeHexagons: (hexs: HexagonDto[]) => void;
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
}

export const useStore = create<EcomapStore>((set) => ({
  activeView: "heatmap",
  setActiveView: (v) => set({ activeView: v }),

  profileId: null,
  setProfileId: (id) => set({ profileId: id }),

  hexagonRecord: {},
  mergeHexagons: (hexs) =>
    set((state) => {
      const next = { ...state.hexagonRecord };
      hexs.forEach((h) => (next[h.h3Index] = h));
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
    set({ isSimulationActive: false, sessionId: null, ghostMarkers: [] }),
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
}));
