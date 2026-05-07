"use client";

import React, { useState } from "react";
import { useStore } from "@/store/useStore";
import ActiveProfileWidget from "@/components/profile/ActiveProfileWidget";

const menuItems = [
  { id: "heatmap", label: "Heatmap & Scoring", icon: HeatmapIcon },
  { id: "whatif", label: "What-if Simulation", icon: WhatIfIcon },
  { id: "analytics", label: "Analytics", icon: AnalyticsIcon },
  { id: "ai", label: "AI Assistant", icon: AIIcon },
] as const;

const poiItems = [
  { id: "drivers", label: "Drivers", icon: "🟢" },
  { id: "competitors", label: "Competitors", icon: "🔴" },
] as const;

const legend = [
  { color: "#FF2222", label: "0 Saturé" },
  { color: "#FF8800", label: "30 Modéré" },
  { color: "#FFEE00", label: "60 Opportunité" },
  { color: "#00CC44", label: "100 Idéal" },
];

export type SidebarViewId = (typeof menuItems)[number]["id"];

export interface SidebarProps {
  /** When set, view selection is controlled by the parent (dashboard + simulation). */
  activeView?: SidebarViewId;
  onActiveViewChange?: (id: SidebarViewId) => void;
}

export default function Sidebar({ activeView, onActiveViewChange }: SidebarProps = {}) {
  const showHeatmap = useStore((s) => s.showHeatmap);
  const toggleHeatmap = useStore((s) => s.toggleHeatmap);
  const showScoreLabels = useStore((s) => s.showScoreLabels);
  const toggleScoreLabels = useStore((s) => s.toggleScoreLabels);
  const showPoiMarkers = useStore((s) => s.showPoiMarkers);
  const togglePoiMarkers = useStore((s) => s.togglePoiMarkers);
  const poiBusinessFilters = useStore((s) => s.poiBusinessFilters);
  const togglePoiBusinessFilter = useStore((s) => s.togglePoiBusinessFilter);

  const [internalActive, setInternalActive] = useState<SidebarViewId>("heatmap");
  const controlled = activeView != null;
  const currentView = controlled ? activeView! : internalActive;

  const setView = (id: SidebarViewId) => {
    if (controlled && onActiveViewChange) {
      onActiveViewChange(id);
    } else if (!controlled) {
      setInternalActive(id);
    }
  };

  return (
    <aside
      className="w-[220px] shrink-0 overflow-y-auto border-r border-slate-200 bg-white py-5"
    >
      <Section title="View Mode">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentView === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => setView(item.id)}
              className={[
                "mx-2 flex w-[calc(100%-16px)] items-center gap-2 rounded-xl px-3 py-2 text-left text-[13.5px] transition",
                isActive ? "bg-blue-600 text-white" : "text-slate-700 hover:bg-slate-100",
              ].join(" ")}
            >
              <Icon active={isActive} />
              {item.label}
            </button>
          );
        })}
      </Section>

      <Section title="Layer Controls">
        {[
          { label: "Heatmap Overlay", checked: showHeatmap, onChange: toggleHeatmap },
          { label: "Score Labels", checked: showScoreLabels, onChange: toggleScoreLabels },
          { label: "POI Markers", checked: showPoiMarkers, onChange: togglePoiMarkers },
        ].map(({ label, checked, onChange }) => (
          <label
            key={label}
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
            <Checkbox checked={checked} onChange={onChange} />
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
            <Checkbox
              checked={poiBusinessFilters[poi.id as keyof typeof poiBusinessFilters]}
              onChange={() => togglePoiBusinessFilter(poi.id as keyof typeof poiBusinessFilters)}
            />
            <span style={{ fontSize: "14px" }}>{poi.icon}</span>
            {poi.label}
          </label>
        ))}
        <div style={{ padding: "6px 16px 0", fontSize: "11px", color: "#94a3b8", lineHeight: 1.5 }}>
          Filters apply when a commercial profile is active: POIs are shown if they match your profile’s driver/competitor tags.
        </div>
      </Section>

      <Section title="Score Legend">
        {legend.map((l) => (
          <div
            key={l.label}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "9px",
              padding: "3px 16px",
              fontSize: "13px",
              color: "#374151",
            }}
          >
            <span
              style={{
                width: "16px",
                height: "16px",
                borderRadius: "4px",
                background: l.color,
                flexShrink: 0,
                display: "inline-block",
              }}
            />
            {l.label}
          </div>
        ))}
      </Section>

      <Section title="Profil commercial" last>
        <ActiveProfileWidget />
      </Section>
    </aside>
  );
}

function Section({ title, children, last = false }: { title: string; children: React.ReactNode; last?: boolean }) {
  return (
    <div
      className={[
        "px-0",
        last ? "mb-0 border-b-0 pb-0" : "mb-2 border-b border-slate-100 pb-2",
      ].join(" ")}
    >
      <p className="px-4 py-2 text-[11px] font-extrabold uppercase tracking-[0.07em] text-slate-400">{title}</p>
      <div className="flex flex-col gap-0.5">{children}</div>
    </div>
  );
}

function Checkbox({ checked, onChange }: { checked: boolean; onChange: () => void }) {
  return (
    <span
      role="checkbox"
      aria-checked={checked}
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === " " || e.key === "Enter") {
          e.preventDefault();
          onChange();
        }
      }}
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
      {checked && (
        <span style={{ color: "#fff", fontSize: "10px", fontWeight: 700, lineHeight: 1 }}>✓</span>
      )}
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
