"use client";

import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from "recharts";
import { useStore } from "@/store/useStore";
import { getHexColor } from "@/lib/hexagonUtils";

export default function SidePanel() {
  const { selectedHexIndex, hexagonRecord, setSelectedHexIndex } = useStore();
  const hex = selectedHexIndex ? hexagonRecord[selectedHexIndex] : null;

  if (!hex) return null;

  const color = getHexColor(hex.score);
  const data = [
    { name: "Flux", value: Math.round(hex.score * 0.9) },
    { name: "Densité", value: Math.round(hex.score * 0.75) },
    { name: "Concur.", value: Math.round(100 - hex.score * 0.6) },
    { name: "Accès", value: Math.round(hex.score * 0.85) },
  ];

  return (
    <div style={{ position: "absolute", top: "64px", left: "12px", zIndex: 1000, width: "200px", borderRadius: "16px", background: "rgba(255,255,255,0.92)", backdropFilter: "blur(12px)", border: "1px solid rgba(255,255,255,0.4)", boxShadow: "0 8px 32px rgba(0,0,0,0.12)", padding: "14px" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "10px" }}>
        <span style={{ fontSize: "11px", fontWeight: 700, color: "#1e293b" }}>Hexagone sélectionné</span>
        <button
          onClick={() => setSelectedHexIndex(null)}
          style={{ fontSize: "16px", color: "#94a3b8", background: "none", border: "none", cursor: "pointer", lineHeight: 1, padding: "0 2px" }}
        >
          ×
        </button>
      </div>
      <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "12px" }}>
        <div style={{ width: "42px", height: "42px", borderRadius: "10px", background: color, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
          <span style={{ fontSize: "16px", fontWeight: 800, color: "#fff" }}>{hex.score}</span>
        </div>
        <div>
          <div style={{ fontSize: "12px", fontWeight: 700, color: "#374151" }}>Score global</div>
          <div style={{ fontSize: "9px", color: "#94a3b8", fontFamily: "monospace", marginTop: "2px" }}>{hex.h3Index.slice(-10)}</div>
        </div>
      </div>
      <ResponsiveContainer width="100%" height={90}>
        <BarChart data={data} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
          <XAxis dataKey="name" tick={{ fontSize: 9, fill: "#94a3b8" }} axisLine={false} tickLine={false} />
          <YAxis domain={[0, 100]} tick={{ fontSize: 8, fill: "#94a3b8" }} axisLine={false} tickLine={false} />
          <Tooltip
            contentStyle={{ fontSize: "10px", borderRadius: "8px", border: "none", boxShadow: "0 4px 12px rgba(0,0,0,0.1)" }}
          />
          <Bar dataKey="value" radius={[4, 4, 0, 0]}>
            {data.map((_, i) => (
              <Cell key={i} fill={color} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
