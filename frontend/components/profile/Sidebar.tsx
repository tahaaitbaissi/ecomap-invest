"use client";

import { useState } from "react";

const menuItems = [
  { id: "heatmap", label: "Heatmap & Scoring", icon: HeatmapIcon },
  { id: "whatif", label: "What-if Simulation", icon: WhatIfIcon },
  { id: "analytics", label: "Analytics", icon: AnalyticsIcon },
  { id: "ai", label: "AI Assistant", icon: AIIcon },
];

const poiItems = [
  { id: "restaurants", label: "Restaurants", color: "#f97316", icon: "🍽️" },
  { id: "retail", label: "Retail", color: "#a855f7", icon: "🛍️" },
  { id: "offices", label: "Offices", color: "#6b7280", icon: "🏢" },
  { id: "entertain", label: "Entertainment", color: "#64748b", icon: "🎭" },
];

const legend = [
  { color: "#22c55e", label: "81-100 Excellent" },
  { color: "#eab308", label: "61-80 Good" },
  { color: "#f97316", label: "41-60 Fair" },
  { color: "#ef4444", label: "0-40 Poor" },
];

export default function Sidebar() {
  const [active, setActive] = useState("heatmap");
  const [layers, setLayers] = useState({ heatmap: true, labels: true, poi: true });
  const [pois, setPois] = useState({ restaurants: true, retail: true, offices: true, entertain: false });

  const toggleLayer = (k: keyof typeof layers) => setLayers((p) => ({ ...p, [k]: !p[k] }));
  const togglePoi = (k: keyof typeof pois) => setPois((p) => ({ ...p, [k]: !p[k] }));

  return (
    <aside
      style={{
        width: "220px",
        flexShrink: 0,
        background: "#fff",
        borderRight: "1px solid #e2e8f0",
        display: "flex",
        flexDirection: "column",
        overflowY: "auto",
        padding: "20px 0",
      }}
    >
      <Section title="View Mode">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = active === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActive(item.id)}
              style={{
                display: "flex",
                alignItems: "center",
                gap: "10px",
                width: "100%",
                padding: "9px 16px",
                background: isActive ? "#1a56db" : "transparent",
                color: isActive ? "#fff" : "#374151",
                border: "none",
                borderRadius: "10px",
                fontSize: "13.5px",
                fontWeight: isActive ? 600 : 400,
                cursor: "pointer",
                textAlign: "left",
                transition: "all 0.15s",
                margin: "1px 0",
              }}
              onMouseEnter={(e) => {
                if (!isActive) e.currentTarget.style.background = "#f1f5f9";
              }}
              onMouseLeave={(e) => {
                if (!isActive) e.currentTarget.style.background = "transparent";
              }}
            >
              <Icon active={isActive} />
              {item.label}
            </button>
          );
        })}
      </Section>

      <Section title="Layer Controls">
        {[
          { key: "heatmap" as const, label: "Heatmap Overlay" },
          { key: "labels" as const, label: "Score Labels" },
          { key: "poi" as const, label: "POI Markers" },
        ].map(({ key, label }) => (
          <label
            key={key}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "9px",
              cursor: "pointer",
              padding: "3px 16px",
              fontSize: "13.5px",
              color: "#374151",
            }}
          >
            <Checkbox checked={layers[key]} onChange={() => toggleLayer(key)} />
            {label}
          </label>
        ))}
      </Section>

      <Section title="POI Filters">
        {poiItems.map((poi) => (
          <label
            key={poi.id}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "9px",
              cursor: "pointer",
              padding: "3px 16px",
              fontSize: "13.5px",
              color: "#374151",
            }}
          >
            <Checkbox checked={pois[poi.id as keyof typeof pois]} onChange={() => togglePoi(poi.id as keyof typeof pois)} />
            <span style={{ fontSize: "14px" }}>{poi.icon}</span>
            {poi.label}
          </label>
        ))}
      </Section>

      <Section title="Score Legend" last>
        {legend.map((l) => (
          <div
            key={l.label}
            style={{ display: "flex", alignItems: "center", gap: "9px", padding: "3px 16px", fontSize: "13px", color: "#374151" }}
          >
            <span
              style={{ width: "16px", height: "16px", borderRadius: "4px", background: l.color, flexShrink: 0, display: "inline-block" }}
            />
            {l.label}
          </div>
        ))}
      </Section>
    </aside>
  );
}

function Section({ title, children, last = false }: { title: string; children: React.ReactNode; last?: boolean }) {
  return (
    <div style={{ marginBottom: last ? 0 : "8px", paddingBottom: last ? 0 : "8px", borderBottom: last ? "none" : "1px solid #f1f5f9" }}>
      <p
        style={{
          fontSize: "11px",
          fontWeight: 700,
          color: "#94a3b8",
          textTransform: "uppercase",
          letterSpacing: "0.07em",
          padding: "8px 16px 8px",
        }}
      >
        {title}
      </p>
      <div style={{ display: "flex", flexDirection: "column", gap: "2px" }}>{children}</div>
    </div>
  );
}

function Checkbox({ checked, onChange }: { checked: boolean; onChange: () => void }) {
  return (
    <span
      onClick={onChange}
      style={{
        width: "16px",
        height: "16px",
        borderRadius: "4px",
        flexShrink: 0,
        border: checked ? "none" : "1.5px solid #d1d5db",
        background: checked ? "#1a56db" : "#fff",
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        transition: "all 0.15s",
        cursor: "pointer",
      }}
    >
      {checked && <span style={{ color: "#fff", fontSize: "10px", fontWeight: 700, lineHeight: 1 }}>✓</span>}
    </span>
  );
}

function HeatmapIcon({ active }: { active: boolean }) {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={active ? "#fff" : "#6b7280"} strokeWidth="2">
      <rect x="3" y="3" width="7" height="7" rx="1" />
      <rect x="14" y="3" width="7" height="7" rx="1" />
      <rect x="3" y="14" width="7" height="7" rx="1" />
      <rect x="14" y="14" width="7" height="7" rx="1" />
    </svg>
  );
}
function WhatIfIcon({ active }: { active: boolean }) {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={active ? "#fff" : "#6b7280"} strokeWidth="2">
      <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
    </svg>
  );
}
function AnalyticsIcon({ active }: { active: boolean }) {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={active ? "#fff" : "#6b7280"} strokeWidth="2">
      <line x1="18" y1="20" x2="18" y2="10" />
      <line x1="12" y1="20" x2="12" y2="4" />
      <line x1="6" y1="20" x2="6" y2="14" />
    </svg>
  );
}
function AIIcon({ active }: { active: boolean }) {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={active ? "#fff" : "#6b7280"} strokeWidth="2">
      <circle cx="12" cy="12" r="3" />
      <path d="M12 1v4M12 19v4M4.22 4.22l2.83 2.83M16.95 16.95l2.83 2.83M1 12h4M19 12h4M4.22 19.78l2.83-2.83M16.95 7.05l2.83-2.83" />
    </svg>
  );
}
