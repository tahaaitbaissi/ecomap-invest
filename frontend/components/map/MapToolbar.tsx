"use client";

import { useState } from "react";
import { useStore } from "@/store/useStore";

interface MapToolbarProps {
  showPoi: boolean;
  showHexagons: boolean;
  onTogglePoi: () => void;
  onToggleHexagons: () => void;
}

export default function MapToolbar({ showPoi, showHexagons, onTogglePoi, onToggleHexagons }: MapToolbarProps) {
  const { isSimulationActive, startSimulation, resetSimulation } = useStore();
  const [showLayers, setShowLayers] = useState(false);

  return (
    <div style={{ position: "absolute", top: "64px", right: "12px", zIndex: 1000, display: "flex", flexDirection: "column", alignItems: "flex-end", gap: "6px" }}>
      <div>
        <button onClick={() => setShowLayers((v) => !v)} style={glassBtn(false)}>
          <LayersIcon /> Layers
        </button>
        {showLayers && (
          <div style={{ marginTop: "4px", borderRadius: "14px", border: "1px solid rgba(255,255,255,0.35)", background: "rgba(255,255,255,0.88)", backdropFilter: "blur(12px)", padding: "10px 14px", boxShadow: "0 8px 24px rgba(0,0,0,0.12)", display: "flex", flexDirection: "column", gap: "8px", minWidth: "160px" }}>
            <ToggleRow label="POI Markers" checked={showPoi} onChange={onTogglePoi} />
            <ToggleRow label="Hexagon Layer" checked={showHexagons} onChange={onToggleHexagons} />
          </div>
        )}
      </div>
      <button onClick={() => (isSimulationActive ? resetSimulation() : startSimulation())} style={glassBtn(isSimulationActive)}>
        <SimIcon />
        {isSimulationActive ? "Quitter simulation" : "Mode simulation"}
      </button>
      {isSimulationActive && (
        <button onClick={resetSimulation} style={{ display: "flex", alignItems: "center", gap: "6px", padding: "7px 12px", borderRadius: "12px", border: "1px solid rgba(239,68,68,0.3)", background: "rgba(239,68,68,0.85)", backdropFilter: "blur(12px)", color: "#fff", fontSize: "12px", fontWeight: 600, cursor: "pointer", boxShadow: "0 4px 12px rgba(0,0,0,0.1)" }}>
          Réinitialiser
        </button>
      )}
    </div>
  );
}

function glassBtn(active: boolean): React.CSSProperties {
  return { display: "flex", alignItems: "center", gap: "6px", padding: "7px 12px", borderRadius: "12px", border: active ? "1px solid rgba(147,51,234,0.3)" : "1px solid rgba(255,255,255,0.35)", background: active ? "rgba(147,51,234,0.88)" : "rgba(255,255,255,0.88)", backdropFilter: "blur(12px)", color: active ? "#fff" : "#374151", fontSize: "12px", fontWeight: 600, cursor: "pointer", boxShadow: "0 4px 12px rgba(0,0,0,0.1)", whiteSpace: "nowrap" };
}

function ToggleRow({ label, checked, onChange }: { label: string; checked: boolean; onChange: () => void }) {
  return (
    <label style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "32px", fontSize: "12px", color: "#374151", cursor: "pointer" }}>
      {label}
      <span onClick={onChange} style={{ position: "relative", display: "inline-flex", width: "28px", height: "16px", borderRadius: "20px", background: checked ? "#1a56db" : "#cbd5e1", transition: "background 0.2s", flexShrink: 0 }}>
        <span style={{ position: "absolute", top: "2px", left: checked ? "14px" : "2px", width: "12px", height: "12px", borderRadius: "50%", background: "#fff", transition: "left 0.2s", boxShadow: "0 1px 3px rgba(0,0,0,0.2)" }} />
      </span>
    </label>
  );
}

function LayersIcon() {
  return <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polygon points="12 2 2 7 12 12 22 7 12 2" /><polyline points="2 17 12 22 22 17" /><polyline points="2 12 12 17 22 12" /></svg>;
}
function SimIcon() {
  return <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /></svg>;
}
