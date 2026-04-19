"use client";

import { useState } from "react";

export default function MapLegend() {
  const [visible, setVisible] = useState(true);
  return (
    <div style={{ position: "absolute", bottom: "32px", right: "12px", zIndex: 1000, display: "flex", flexDirection: "column", alignItems: "flex-end", gap: "4px" }}>
      <button onClick={() => setVisible((v) => !v)} style={{ padding: "4px 10px", borderRadius: "8px", border: "1px solid rgba(255,255,255,0.35)", background: "rgba(255,255,255,0.85)", backdropFilter: "blur(10px)", fontSize: "10px", color: "#64748b", cursor: "pointer", boxShadow: "0 2px 8px rgba(0,0,0,0.08)" }}>
        {visible ? "Masquer" : "Légende"}
      </button>
      {visible && (
        <div style={{ borderRadius: "14px", border: "1px solid rgba(255,255,255,0.35)", background: "rgba(255,255,255,0.88)", backdropFilter: "blur(12px)", padding: "12px 14px", boxShadow: "0 8px 24px rgba(0,0,0,0.12)", minWidth: "170px" }}>
          <p style={{ fontSize: "11px", fontWeight: 700, color: "#374151", marginBottom: "8px" }}>Score Legend</p>
          <div style={{ height: "10px", borderRadius: "6px", background: "linear-gradient(to right,#FF2222,#FF8800,#FFA500,#FFEE00,#AEDD00,#00CC44)", marginBottom: "6px" }} />
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: "9px", color: "#94a3b8", lineHeight: 1.3 }}>
            <span>0<br />Saturé</span>
            <span style={{ textAlign: "center" }}>30<br />Modéré</span>
            <span style={{ textAlign: "center" }}>60<br />Opportunité</span>
            <span style={{ textAlign: "right" }}>100<br />Idéal</span>
          </div>
        </div>
      )}
    </div>
  );
}
