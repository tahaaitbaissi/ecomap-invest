"use client";

import React, { useState } from "react";
import { BarChart3, Bot, Layers3, Sparkles } from "lucide-react";
import { useStore } from "@/store/useStore";
import ActiveProfileWidget from "@/components/profile/ActiveProfileWidget";

const menuItems = [
  { id: "heatmap", label: "Heatmap & Scoring", icon: Layers3 },
  { id: "whatif", label: "What-if Simulation", icon: Sparkles },
  { id: "analytics", label: "Analytics", icon: BarChart3 },
  { id: "ai", label: "AI Assistant", icon: Bot },
] as const;

const poiItems = [
  { id: "drivers", label: "Drivers" },
  { id: "competitors", label: "Competitors" },
] as const;

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
    <aside className="flex h-full w-[240px] shrink-0 flex-col border-r border-[color:var(--color-border)] bg-[color:var(--color-bg-card)]">
      <div className="flex-1 overflow-y-auto py-5">
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
                  "relative mx-2 flex w-[calc(100%-16px)] items-center gap-2 rounded-xl px-3 py-2 text-left text-[13.5px] transition",
                  isActive
                    ? [
                        "bg-[color:var(--color-accent-surface)] text-[color:var(--color-text-primary)] ring-1 ring-[color:var(--color-accent-border)]",
                        "before:absolute before:left-0 before:top-1.5 before:bottom-1.5 before:w-[3px] before:rounded-full",
                        "before:bg-[color:var(--color-accent)]",
                      ].join(" ")
                    : "text-[color:var(--color-text-secondary)] hover:bg-[color:rgba(234,240,255,0.04)] hover:text-[color:var(--color-text-primary)]",
                ].join(" ")}
              >
                <Icon
                  className={[
                    "h-4 w-4",
                    isActive ? "text-[color:var(--color-text-primary)]" : "text-[color:var(--color-text-muted)]",
                  ].join(" ")}
                />
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
              className="flex cursor-pointer items-center gap-2 px-4 py-1 text-[13.5px] text-[color:var(--color-text-secondary)]"
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
              className="flex cursor-pointer items-center gap-2 px-4 py-1 text-[13.5px] text-[color:var(--color-text-secondary)]"
            >
              <Checkbox
                checked={poiBusinessFilters[poi.id as keyof typeof poiBusinessFilters]}
                onChange={() => togglePoiBusinessFilter(poi.id as keyof typeof poiBusinessFilters)}
              />
              {poi.label}
            </label>
          ))}
          <div className="px-4 pt-1 text-[11px] leading-relaxed text-[color:var(--color-text-muted)]">
            Filters apply when a commercial profile is active: POIs are shown if they match your profile’s driver/competitor tags.
          </div>
        </Section>

        {/* Score legend removed (UI already self-explanatory). */}
      </div>

      <div className="sticky bottom-0 border-t border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] py-3 shadow-[0_-8px_24px_rgba(0,0,0,0.06)]">
        <p className="px-4 pb-2 text-[11px] font-extrabold uppercase tracking-[0.07em] text-[color:var(--color-text-muted)]">
          Profil commercial
        </p>
        <ActiveProfileWidget />
      </div>
    </aside>
  );
}

function Section({ title, children, last = false }: { title: string; children: React.ReactNode; last?: boolean }) {
  return (
    <div
      className={[
        "px-0",
        last ? "mb-0 border-b-0 pb-0" : "mb-2 border-b border-[color:var(--color-border)] pb-2",
      ].join(" ")}
    >
      <p className="px-4 py-2 text-[11px] font-extrabold uppercase tracking-[0.07em] text-[color:var(--color-text-muted)]">{title}</p>
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
      className={[
        "inline-flex h-4 w-4 shrink-0 cursor-pointer items-center justify-center rounded transition",
        checked
          ? "bg-[color:var(--color-accent)] text-[color:var(--color-bg-card)]"
          : "border border-[color:var(--color-border)] bg-transparent text-transparent hover:bg-[color:rgba(234,240,255,0.04)]",
      ].join(" ")}
    >
      {checked && <span className="text-[10px] font-extrabold leading-none">✓</span>}
    </span>
  );
}

