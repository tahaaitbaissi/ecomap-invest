"use client";

import { useEffect, useState, useMemo } from "react";
import {
  LineChart, Line, XAxis, YAxis, Tooltip as RTooltip, ResponsiveContainer,
  PieChart, Pie, Cell,
} from "recharts";
import { useStore } from "@/store/useStore";
import { runSimulation } from "@/services/api/simulationService";
import { getHexColor } from "@/lib/hexagonUtils";
import { useSSEExplanation } from "@/hooks/useSSEExplanation";
import TypewriterText from "@/components/profile/TypewriterText";

export default function RightPanel() {
  const activeView = useStore((s) => s.activeView);
  return (
    <aside style={{ width: "280px", flexShrink: 0, background: "#fff", borderLeft: "1px solid #e2e8f0", display: "flex", flexDirection: "column", overflowY: "auto" }}>
      <div style={{ padding: "20px 16px 12px", borderBottom: "1px solid #f1f5f9" }}>
        <h2 style={{ fontSize: "16px", fontWeight: 700, color: "#1e293b", margin: 0 }}>
          {activeView === "heatmap" ? "Hex Details"
            : activeView === "whatif" ? "What-if Simulation"
            : activeView === "analytics" ? "Analytics Dashboard"
            : "AI Assistant"}
        </h2>
      </div>
      <div style={{ flex: 1, overflowY: "auto", paddingTop: "16px" }}>
        {activeView === "heatmap" && <HexDetailsPanel />}
        {activeView === "whatif" && <WhatIfPanel />}
        {activeView === "analytics" && <AnalyticsPanel />}
        {activeView === "ai" && <AIPanel />}
      </div>
    </aside>
  );
}

/* ---- Hex Details ---- */
function HexDetailsPanel() {
  const { selectedHexIndex, hexagonRecord, profileId } = useStore();
  const hex = selectedHexIndex ? hexagonRecord[selectedHexIndex] : null;
  const { text, isStreaming, isComplete, error, start, stop } = useSSEExplanation(
    selectedHexIndex,
    profileId,
    hex?.score ?? 50
  );

  // Auto-start analysis whenever a new hexagon is selected
  useEffect(() => {
    if (selectedHexIndex) start();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedHexIndex]);

  if (!hex) {
    return (
      <div style={{ padding: "32px 16px", textAlign: "center", color: "#94a3b8", fontSize: "13px", lineHeight: 1.7 }}>
        Cliquez sur un hexagone<br />pour voir les détails
      </div>
    );
  }
  const label = hex.score >= 80 ? "Excellent" : hex.score >= 60 ? "Good" : hex.score >= 40 ? "Fair" : "Poor";
  const hexNum = parseInt(hex.h3Index.slice(-3), 16) % 100;

  // Deterministic mock POIs derived from h3Index
  const seed = parseInt(hex.h3Index.slice(-4), 16);
  const poiNames = ["Le Marrakchi", "Café Atlas", "Boutique Moda", "Épicerie Centrale", "Pharmacie du Parc", "Boulangerie Paul", "Sushi House", "Pizza Roma", "Hammam Luxe", "Librairie Ibn Rushd"];
  const poiTypes = ["🍽️ Restaurant", "☕ Café", "🛍️ Retail", "🏪 Épicerie", "💊 Pharmacie", "🥐 Boulangerie", "🍱 Sushi", "🍕 Pizzeria", "💆 Spa", "📚 Librairie"];
  const topPois = Array.from({ length: 3 }, (_, i) => ({
    name: poiNames[(seed + i * 3) % poiNames.length],
    type: poiTypes[(seed + i) % poiTypes.length],
    rating: (3.5 + ((seed + i * 7) % 15) / 10).toFixed(1),
    distance: `${100 + ((seed + i * 41) % 400)}m`,
  }));

  return (
    <>
    <style>{`
      @keyframes slideInRight { from { opacity:0; transform:translateX(20px); } to { opacity:1; transform:translateX(0); } }
    `}</style>
    <div style={{ padding: "0 16px 20px", animation: "slideInRight 0.28s ease-out" }}>
      <div style={{ borderRadius: "14px", background: "linear-gradient(135deg, #1a56db 0%, #2563eb 100%)", padding: "18px 16px", marginBottom: "16px", color: "#fff" }}>
        <div style={{ fontSize: "11px", fontWeight: 600, opacity: 0.8, marginBottom: "8px", textTransform: "uppercase", letterSpacing: "0.05em" }}>Investment Score</div>
        <div style={{ display: "flex", alignItems: "baseline", gap: "4px", marginBottom: "12px" }}>
          <span style={{ fontSize: "44px", fontWeight: 800, lineHeight: 1 }}>{hex.score}</span>
          <span style={{ fontSize: "18px", opacity: 0.6 }}>/100</span>
        </div>
        <div style={{ display: "flex", gap: "6px" }}>
          <span style={{ background: "rgba(255,255,255,0.22)", borderRadius: "20px", padding: "3px 10px", fontSize: "11px", fontWeight: 600 }}>{label}</span>
          <span style={{ background: "rgba(255,255,255,0.22)", borderRadius: "20px", padding: "3px 10px", fontSize: "11px" }}>Hex #{hexNum}</span>
        </div>
      </div>
      <div style={{ marginBottom: "14px", padding: "12px", background: "#f8fafc", borderRadius: "10px" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "6px" }}>
          <InfoIcon />
          <span style={{ fontSize: "12px", fontWeight: 700, color: "#374151" }}>About Score</span>
        </div>
        <p style={{ fontSize: "11px", color: "#6b7280", lineHeight: 1.6, margin: 0 }}>
          Investment score combines demographics, competition, foot traffic, and market trends to rate location potential.
        </p>
      </div>
      <div style={{ marginBottom: "14px" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "8px" }}>
          <TrendUpIcon color="#374151" />
          <span style={{ fontSize: "12px", fontWeight: 700, color: "#374151" }}>Positive Drivers</span>
        </div>
        {["High foot traffic", "Low competition", "Premium demographics", "Strong demand"].map((d) => (
          <div key={d} style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "5px" }}>
            <span style={{ color: "#22c55e", fontSize: "13px", fontWeight: 700 }}>✓</span>
            <span style={{ fontSize: "12px", color: "#374151" }}>{d}</span>
          </div>
        ))}
      </div>
      <div style={{ marginBottom: "10px", padding: "12px", background: "#f8fafc", borderRadius: "10px" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "4px" }}>
          <CompetIcon />
          <span style={{ fontSize: "12px", fontWeight: 700, color: "#374151" }}>Competition</span>
        </div>
        <span style={{ fontSize: "13px", color: "#374151" }}>2 competitors within 1km</span>
      </div>
      <div style={{ marginBottom: "20px", padding: "12px", background: "#f8fafc", borderRadius: "10px" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "6px" }}>
          <DemoIcon />
          <span style={{ fontSize: "12px", fontWeight: 700, color: "#374151" }}>Demographics</span>
        </div>
        <div style={{ fontSize: "24px", fontWeight: 800, color: "#1e293b", lineHeight: 1 }}>4 222</div>
        <div style={{ fontSize: "11px", color: "#94a3b8", marginTop: "2px" }}>residents/km²</div>
      </div>
      {/* Top POIs */}
      <div style={{ marginBottom: "16px" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "8px" }}>
          <span style={{ fontSize: "13px" }}>📍</span>
          <span style={{ fontSize: "12px", fontWeight: 700, color: "#374151" }}>Top POIs à proximité</span>
        </div>
        {topPois.map((poi, i) => (
          <div key={i} style={{ display: "flex", alignItems: "center", gap: "10px", padding: "8px 10px", background: "#f8fafc", borderRadius: "8px", marginBottom: "6px" }}>
            <span style={{ fontSize: "18px", flexShrink: 0 }}>{poi.type.split(" ")[0]}</span>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: "12px", fontWeight: 600, color: "#1e293b", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{poi.name}</div>
              <div style={{ fontSize: "10px", color: "#94a3b8" }}>{poi.type.split(" ").slice(1).join(" ")} · {poi.distance}</div>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: "3px", flexShrink: 0 }}>
              <span style={{ fontSize: "11px", color: "#f59e0b" }}>⭐</span>
              <span style={{ fontSize: "11px", fontWeight: 700, color: "#374151" }}>{poi.rating}</span>
            </div>
          </div>
        ))}
      </div>

      <TypewriterText
        text={text}
        isStreaming={isStreaming}
        isComplete={isComplete}
        error={error}
        onStop={stop}
        onRegenerate={start}
      />

      <button style={{ width: "100%", padding: "12px", background: "#1a56db", color: "#fff", border: "none", borderRadius: "10px", fontSize: "13px", fontWeight: 600, cursor: "pointer", marginTop: "14px" }}>
        View Full Report
      </button>
    </div>
    </>
  );
}

/* ---- What-if ---- */
function WhatIfPanel() {
  const { isSimulationActive, startSimulation, resetSimulation, ghostMarkers, sessionId } = useStore();
  const [running, setRunning] = useState(false);
  const handleRun = async () => {
    if (!sessionId || ghostMarkers.length === 0) return;
    setRunning(true);
    try { await runSimulation(sessionId, ghostMarkers); } catch { } finally { setRunning(false); }
  };
  return (
    <div style={{ padding: "0 16px 20px" }}>
      <div style={{ borderRadius: "14px", background: "linear-gradient(135deg, #7c3aed 0%, #a855f7 100%)", padding: "24px 20px", marginBottom: "16px", color: "#fff", textAlign: "center" }}>
        <div style={{ fontSize: "32px", marginBottom: "10px" }}>📍</div>
        <div style={{ fontSize: "14px", fontWeight: 700, marginBottom: "6px" }}>Drop a New Business Marker</div>
        <div style={{ fontSize: "11px", opacity: 0.85, lineHeight: 1.6 }}>Click anywhere on the map to simulate a new business location</div>
      </div>
      {ghostMarkers.length > 0 && (
        <div style={{ marginBottom: "14px" }}>
          <p style={{ fontSize: "10px", fontWeight: 700, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: "8px" }}>Marqueurs placés</p>
          {ghostMarkers.map((m) => (
            <div key={m.id} style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "6px", padding: "8px 10px", background: "#f8fafc", borderRadius: "8px" }}>
              <span style={{ width: "12px", height: "12px", borderRadius: "50%", background: m.type === "competitor" ? "#ef4444" : "#22c55e", flexShrink: 0 }} />
              <span style={{ fontSize: "12px", color: "#374151", fontWeight: 500 }}>{m.tag}</span>
              <span style={{ fontSize: "10px", color: "#94a3b8", marginLeft: "auto" }}>{m.type}</span>
            </div>
          ))}
        </div>
      )}
      <button
        onClick={isSimulationActive ? handleRun : startSimulation}
        disabled={isSimulationActive && (ghostMarkers.length === 0 || running)}
        style={{ width: "100%", padding: "12px", background: isSimulationActive && (ghostMarkers.length === 0 || running) ? "#94a3b8" : "#7c3aed", color: "#fff", border: "none", borderRadius: "10px", fontSize: "13px", fontWeight: 600, cursor: "pointer", marginBottom: "8px" }}
      >
        {running ? "Simulation en cours..." : isSimulationActive ? "Run Simulation" : "Start Simulation"}
      </button>
      {isSimulationActive && (
        <button onClick={resetSimulation} style={{ width: "100%", padding: "10px", background: "none", color: "#ef4444", border: "1px solid #ef4444", borderRadius: "10px", fontSize: "12px", cursor: "pointer" }}>
          Réinitialiser
        </button>
      )}
    </div>
  );
}

/* ---- Analytics ---- */
function AnalyticsPanel() {
  const hexagonRecord = useStore((s) => s.hexagonRecord);
  const hexs = useMemo(() => Object.values(hexagonRecord), [hexagonRecord]);
  const avg = hexs.length > 0 ? Math.round(hexs.reduce((s, h) => s + h.score, 0) / hexs.length) : 78;
  const monthlyData = [
    { month: "Feb", score: 65 }, { month: "Apr", score: 68 },
    { month: "Jun", score: 70 }, { month: "Aug", score: 72 },
    { month: "Oct", score: 74 }, { month: "Dec", score: avg },
  ];
  const distData = useMemo(() => {
    const b = hexs.length > 0 ? hexs : [];
    return [
      { name: "Excellent (81-100)", value: b.filter((h) => h.score > 80).length || 3, color: "#22c55e" },
      { name: "Good (61-80)", value: b.filter((h) => h.score > 60 && h.score <= 80).length || 3, color: "#eab308" },
      { name: "Fair (41-60)", value: b.filter((h) => h.score > 40 && h.score <= 60).length || 4, color: "#f97316" },
      { name: "Poor (0-40)", value: b.filter((h) => h.score <= 40).length || 5, color: "#ef4444" },
    ];
  }, [hexs]);
  return (
    <div style={{ padding: "0 16px 20px" }}>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px", marginBottom: "20px" }}>
        <div style={{ padding: "14px 12px", background: "#f8fafc", borderRadius: "10px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "4px", marginBottom: "4px" }}>
            <TrendUpIcon color="#1a56db" /><span style={{ fontSize: "10px", color: "#94a3b8" }}>Avg Score</span>
          </div>
          <div style={{ fontSize: "24px", fontWeight: 800, color: "#1e293b" }}>{avg}</div>
        </div>
        <div style={{ padding: "14px 12px", background: "#f8fafc", borderRadius: "10px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "4px", marginBottom: "4px" }}>
            <TrendUpIcon color="#22c55e" /><span style={{ fontSize: "10px", color: "#94a3b8" }}>Growth</span>
          </div>
          <div style={{ fontSize: "24px", fontWeight: 800, color: "#22c55e" }}>+12%</div>
        </div>
      </div>
      <p style={{ fontSize: "12px", fontWeight: 700, color: "#374151", marginBottom: "10px" }}>Performance Trends (Monthly)</p>
      <ResponsiveContainer width="100%" height={110}>
        <LineChart data={monthlyData} margin={{ top: 4, right: 8, left: -16, bottom: 0 }}>
          <XAxis dataKey="month" tick={{ fontSize: 9, fill: "#94a3b8" }} axisLine={false} tickLine={false} />
          <YAxis domain={[60, 80]} tick={{ fontSize: 9, fill: "#94a3b8" }} axisLine={false} tickLine={false} />
          <RTooltip contentStyle={{ fontSize: "10px", borderRadius: "8px", border: "none", boxShadow: "0 4px 12px rgba(0,0,0,0.1)" }} />
          <Line type="monotone" dataKey="score" stroke="#1a56db" strokeWidth={2} dot={{ r: 3, fill: "#1a56db" }} />
        </LineChart>
      </ResponsiveContainer>
      <p style={{ fontSize: "12px", fontWeight: 700, color: "#374151", margin: "20px 0 10px" }}>Score Distribution</p>
      <ResponsiveContainer width="100%" height={150}>
        <PieChart>
          <Pie data={distData} cx="50%" cy="50%" innerRadius={42} outerRadius={62} dataKey="value" paddingAngle={2}>
            {distData.map((entry, i) => <Cell key={i} fill={entry.color} />)}
          </Pie>
          <RTooltip
            contentStyle={{ fontSize: "10px", borderRadius: "8px", border: "none", boxShadow: "0 4px 12px rgba(0,0,0,0.1)" }}
            formatter={(v) => [v ?? 0, ""]}
          />
        </PieChart>
      </ResponsiveContainer>
      <div style={{ display: "flex", flexDirection: "column", gap: "5px", marginTop: "8px" }}>
        {distData.map((d) => (
          <div key={d.name} style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
              <span style={{ width: "10px", height: "10px", borderRadius: "2px", background: d.color, flexShrink: 0 }} />
              <span style={{ fontSize: "10px", color: "#374151" }}>{d.name}</span>
            </div>
            <span style={{ fontSize: "10px", fontWeight: 700, color: "#374151" }}>{d.value}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

/* ---- AI Assistant ---- */
function AIPanel() {
  const { selectedHexIndex, hexagonRecord } = useStore();
  const hex = selectedHexIndex ? hexagonRecord[selectedHexIndex] : null;
  const [input, setInput] = useState("");
  const hexNum = hex ? parseInt(hex.h3Index.slice(-3), 16) % 100 : null;
  return (
    <div style={{ display: "flex", flexDirection: "column", padding: "0 16px 16px", height: "calc(100vh - 150px)" }}>
      <div style={{ flex: 1, overflowY: "auto", marginBottom: "12px" }}>
        {hex ? (
          <div style={{ display: "flex", gap: "10px" }}>
            <div style={{ width: "32px", height: "32px", borderRadius: "50%", background: "linear-gradient(135deg, #7c3aed, #1a56db)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <span style={{ color: "#fff", fontSize: "14px" }}>✦</span>
            </div>
            <div style={{ background: "#f8fafc", borderRadius: "12px", borderTopLeftRadius: "4px", padding: "12px 14px", flex: 1 }}>
              <p style={{ fontSize: "12px", color: "#374151", lineHeight: 1.7, margin: 0 }}>
                For hex #{hexNum} (score {hex.score}), this area has strong potential.
                <br /><br />
                Drivers: High foot traffic, Low competition.
                <br />
                Competition: 2 competitors within 1km.
              </p>
              <p style={{ fontSize: "10px", color: "#94a3b8", marginTop: "8px", marginBottom: 0 }}>Just now</p>
            </div>
          </div>
        ) : (
          <p style={{ fontSize: "12px", color: "#94a3b8", textAlign: "center", marginTop: "24px", lineHeight: 1.7 }}>
            Sélectionnez un hexagone<br />pour obtenir une analyse AI
          </p>
        )}
      </div>
      <div style={{ display: "flex", gap: "8px", flexShrink: 0 }}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask me anything about locations..."
          style={{ flex: 1, padding: "10px 14px", borderRadius: "10px", border: "1.5px solid #e2e8f0", fontSize: "12px", outline: "none", color: "#374151" }}
        />
        <button style={{ width: "38px", height: "38px", borderRadius: "10px", background: "#1a56db", border: "none", color: "#fff", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" />
          </svg>
        </button>
      </div>
    </div>
  );
}

/* ---- Icons ---- */
function InfoIcon() {
  return <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth="2"><circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" /></svg>;
}
function TrendUpIcon({ color = "#374151" }: { color?: string }) {
  return <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18" /><polyline points="17 6 23 6 23 12" /></svg>;
}
function CompetIcon() {
  return <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f97316" strokeWidth="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" /></svg>;
}
function DemoIcon() {
  return <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#8b5cf6" strokeWidth="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" /></svg>;
}
