"use client";

import { useEffect, useState, useMemo } from "react";
import {
  Tooltip as RTooltip, ResponsiveContainer,
  PieChart, Pie, Cell,
} from "recharts";
import { useStore } from "@/store/useStore";
import AiChatPanel from "@/components/ai/AiChatPanel";
import {
  fetchHexDetails,
  type HexExplanationContextDto,
  type HexTagContributionRow,
} from "@/services/api/profileService";
import { computeViewportScoreStats } from "@/lib/analyticsViewportStats";
import { fetchZoneStats, type ZoneStatsResponse } from "@/services/api/analyticsService";

export default function RightPanel() {
  const activeView = useStore((s) => s.activeView);
  return (
    <aside className="w-[320px] shrink-0 overflow-y-auto border-l border-[color:var(--color-border)] bg-[color:var(--color-bg-card)]">
      <div className="ds-accent-underline border-b border-[color:var(--color-border)] px-4 pb-3 pt-5">
        <h2 className="m-0 flex items-center gap-2 text-base font-extrabold text-[color:var(--color-text-primary)]">
          <span className="h-2 w-2 rounded-full bg-[color:var(--color-accent)]" />
          {activeView === "heatmap" ? "Hex Details"
            : activeView === "whatif" ? "What-if Simulation"
            : activeView === "analytics" ? "Analytics Dashboard"
            : "AI Assistant"}
        </h2>
      </div>
      <div className="flex-1 overflow-y-auto pt-4">
        {activeView === "heatmap" && <HexDetailsPanel />}
        {activeView === "whatif" && <WhatIfPanel />}
        {activeView === "analytics" && <AnalyticsPanel />}
        {activeView === "ai" && <AiChatPanel />}
      </div>
    </aside>
  );
}

/* ---- Hex Details ---- */
function HexDetailsPanel() {
  const { selectedHexIndex, hexagonRecord, profileId, selectedCommercialProfile } = useStore();
  const hex = selectedHexIndex ? hexagonRecord[selectedHexIndex] : null;
  const viewportCellCount = Object.keys(hexagonRecord).length;

  const [ctx, setCtx] = useState<HexExplanationContextDto | null>(null);
  const [ctxLoading, setCtxLoading] = useState(false);
  const [ctxError, setCtxError] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedHexIndex || !profileId) {
      setCtx(null);
      setCtxError(null);
      return;
    }
    let cancelled = false;
    setCtxLoading(true);
    setCtxError(null);
    void fetchHexDetails(profileId, selectedHexIndex, viewportCellCount || undefined)
      .then((c) => {
        if (!cancelled) setCtx(c);
      })
      .catch((e: unknown) => {
        const msg =
          e && typeof e === "object" && "response" in e
            ? (e as { response?: { status?: number; data?: string | { message?: string } } }).response
                ?.data
            : undefined;
        const str =
          typeof msg === "string"
            ? msg
            : msg && typeof msg === "object" && "message" in msg
              ? String((msg as { message?: string }).message)
              : "Impossible de charger le détail du score.";
        if (!cancelled) setCtxError(str);
      })
      .finally(() => {
        if (!cancelled) setCtxLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedHexIndex, profileId, viewportCellCount]);

  if (!hex) {
    return (
      <div className="px-4 py-8 text-center text-[13px] leading-relaxed text-[color:var(--color-text-muted)]">
        Cliquez sur un hexagone<br />pour voir les détails
      </div>
    );
  }
  const scoreVal = hex.score;
  const label =
    scoreVal == null
      ? "Aperçu"
      : scoreVal >= 80
        ? "Excellent"
        : scoreVal >= 60
          ? "Good"
          : scoreVal >= 40
            ? "Fair"
            : "Poor";
  const hexNum = parseInt(hex.h3Index.slice(-3), 16) % 100;

  const driverRows = ctx?.drivers
    ? [...ctx.drivers].sort(
        (a, b) => b.weightedContributionAcrossLeaves - a.weightedContributionAcrossLeaves,
      )
    : [];
  const compRows = ctx?.competitors
    ? [...ctx.competitors].sort(
        (a, b) => b.weightedContributionAcrossLeaves - a.weightedContributionAcrossLeaves,
      )
    : [];

  const formatTagLine = (tag: HexTagContributionRow) =>
    `${tag.tag} · poi ${tag.countInsideAcrossLeaves} × ${tag.weight.toFixed(2)} → ${tag.weightedContributionAcrossLeaves.toFixed(2)}`;

  const explainBlocked = !profileId;

  return (
    <>
      <style>{`
      @keyframes slideInRight { from { opacity:0; transform:translateX(20px); } to { opacity:1; transform:translateX(0); } }
    `}</style>
      <div className="px-4 pb-5" style={{ animation: "slideInRight 0.28s ease-out" }}>
        <div className="mb-4 rounded-[14px] border border-[color:rgba(47,107,255,0.25)] bg-[color:rgba(47,107,255,0.12)] p-4">
          <div className="mb-2 text-[11px] font-semibold uppercase tracking-[0.05em] text-[color:var(--color-text-secondary)]">Investment Score</div>
          <div className="mb-3 flex items-baseline gap-1">
            <span className="text-[44px] font-extrabold leading-none text-[color:var(--color-text-primary)]">
              {scoreVal == null ? "—" : Math.round(scoreVal)}
            </span>
            <span className="text-[18px] text-[color:var(--color-text-muted)]">/100</span>
          </div>
          <div className="flex flex-wrap gap-2">
            <span className="rounded-full border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.04)] px-2.5 py-1 text-[11px] font-semibold text-[color:var(--color-text-primary)]">{label}</span>
            <span className="rounded-full border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.04)] px-2.5 py-1 text-[11px] text-[color:var(--color-text-secondary)]">Hex #{hexNum}</span>
            {ctx?.aggregatedFromGridLeaves ? (
              <span className="rounded-full border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.04)] px-2.5 py-1 text-[11px] text-[color:var(--color-text-secondary)]">Σ {ctx.gridLeafCount} mailles rés. {ctx.gridLeafResolution}</span>
            ) : null}
          </div>
        </div>
        <div className="mb-3 rounded-[12px] border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.03)] p-3">
          <div className="mb-1 flex items-center gap-2">
            <InfoIcon />
            <span className="text-[12px] font-extrabold text-[color:var(--color-text-primary)]">À propos du score</span>
          </div>
          <p className="m-0 text-[11px] leading-relaxed text-[color:var(--color-text-secondary)]">
            {explainBlocked
              ? "Sélectionnez un profil commercial pour scorer et expliquer cette cellule."
              : ctxLoading
                ? "Chargement des faits métier…"
                : ctxError ?? `Profil « ${selectedCommercialProfile?.name ?? ctx?.profileName ?? "—"} » — score affiché 0–100 après normalisation globale (voir « XAI » ci-dessous).`}
          </p>
          {ctx && !ctxLoading && (
            <p className="mt-2 text-[10px] leading-relaxed text-[color:var(--color-text-muted)]">
              Brut moyen (terrain){" "}
              <strong>{ctx.averageRawAcrossLeaves.toFixed(4)}</strong> ; plage stretch{" "}
              <strong>
                [{ctx.normalizationStretchLow.toFixed(3)}, {ctx.normalizationStretchHigh.toFixed(3)}]
              </strong>
              {ctx.normalizationFlat ? " (distribution plate)." : "."}
            </p>
          )}
        </div>
        <div style={{ marginBottom: "14px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "8px" }}>
            <TrendUpIcon color="#374151" />
            <span style={{ fontSize: "12px", fontWeight: 700, color: "#374151" }}>
              Conducteurs POI{" "}
              {ctx?.aggregatedFromGridLeaves ? "(somme Σ sur sous-mailles)" : ""}
            </span>
          </div>
          {ctxLoading ? (
            <span style={{ fontSize: "12px", color: "#94a3b8" }}>…</span>
          ) : explainBlocked ? null : ctxError ? null : driverRows.filter((t) => t.countInsideAcrossLeaves > 0)
              .length === 0 ? (
            <span style={{ fontSize: "12px", color: "#94a3b8" }}>Aucun conducteur pertinent dans cette cellule.</span>
          ) : (
            driverRows
              .filter((t) => t.countInsideAcrossLeaves > 0)
              .map((t) => (
                <div key={t.tag} style={{ display: "flex", alignItems: "flex-start", gap: "8px", marginBottom: "5px" }}>
                  <span style={{ color: "#22c55e", fontSize: "13px", fontWeight: 700, flexShrink: 0 }}>✓</span>
                  <span style={{ fontSize: "11px", color: "#374151", lineHeight: 1.45 }}>{formatTagLine(t)}</span>
                </div>
              ))
          )}
        </div>
        <div style={{ marginBottom: "10px", padding: "12px", background: "#f8fafc", borderRadius: "10px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "4px" }}>
            <CompetIcon />
            <span style={{ fontSize: "12px", fontWeight: 700, color: "#374151" }}>Concurrents</span>
          </div>
          {ctx && !ctxLoading && !explainBlocked ? (
            <>
              <span style={{ fontSize: "13px", color: "#374151" }}>
                Σ {ctx.totalCompetitorPoisUnweightedAcrossLeaves} POI (tags concurrents, non pondéré)
              </span>
              {compRows.filter((t) => t.countInsideAcrossLeaves > 0).length > 0 ? (
                <div style={{ marginTop: "8px", fontSize: "11px", color: "#64748b", lineHeight: 1.45 }}>
                  {compRows
                    .filter((t) => t.countInsideAcrossLeaves > 0)
                    .slice(0, 8)
                    .map((t) => (
                      <div key={`c-${t.tag}`}>{formatTagLine(t)}</div>
                    ))}
                </div>
              ) : null}
            </>
          ) : (
            <span style={{ fontSize: "12px", color: "#94a3b8" }}>—</span>
          )}
        </div>
        <div style={{ marginBottom: "20px", padding: "12px", background: "#f8fafc", borderRadius: "10px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "6px" }}>
            <DemoIcon />
            <span style={{ fontSize: "12px", fontWeight: 700, color: "#374151" }}>Démographie</span>
          </div>
          {ctx && ctx.populationDensityAvg != null ? (
            <>
              <div style={{ fontSize: "22px", fontWeight: 800, color: "#1e293b", lineHeight: 1 }}>
                {Math.round(ctx.populationDensityAvg).toLocaleString("fr-FR")}
              </div>
              <div style={{ fontSize: "11px", color: "#94a3b8", marginTop: "2px" }}>densité moy. (± km² selon jeu de données)</div>
              {ctx.avgIncomeAvg != null ? (
                <div style={{ fontSize: "11px", color: "#64748b", marginTop: "6px" }}>
                  Revenu moy. est.{" "}
                  <strong>{Math.round(ctx.avgIncomeAvg).toLocaleString("fr-FR")}</strong>
                </div>
              ) : null}
            </>
          ) : ctx && ctx.demographics?.usingDemographics ? (
            <span style={{ fontSize: "12px", color: "#94a3b8" }}>Pas de ligne démo pour ces mailles.</span>
          ) : (
            <span style={{ fontSize: "12px", color: "#94a3b8" }}>Démographie désactivée pour ce scoring.</span>
          )}
          {ctx?.averagePopulationTerm != null ? (
            <div style={{ fontSize: "10px", color: "#94a3b8", marginTop: "6px" }}>
              Terme population (Σ formule carte) ~ {ctx.averagePopulationTerm.toFixed(4)}
            </div>
          ) : null}
        </div>

        <button
          type="button"
          style={{ width: "100%", padding: "12px", background: "#1a56db", color: "#fff", border: "none", borderRadius: "10px", fontSize: "13px", fontWeight: 600, cursor: "pointer", marginTop: "14px" }}
        >
          Exporter (bientôt)
        </button>
      </div>
    </>
  );
}

/* ---- What-if ---- */
function WhatIfPanel() {
  const { ghostMarkers } = useStore();
  return (
    <div style={{ padding: "0 16px 20px" }}>
      <div style={{ borderRadius: "14px", background: "linear-gradient(135deg, #7c3aed 0%, #a855f7 100%)", padding: "24px 20px", marginBottom: "16px", color: "#fff", textAlign: "center" }}>
        <div style={{ fontSize: "32px", marginBottom: "10px" }}>📍</div>
        <div style={{ fontSize: "14px", fontWeight: 700, marginBottom: "6px" }}>Simulation sur la carte</div>
        <div style={{ fontSize: "11px", opacity: 0.85, lineHeight: 1.6 }}>
          Utilisez le mode What-if dans la barre latérale, puis cliquez sur la carte pour placer un point et confirmer dans le panneau flottant.
        </div>
      </div>
      {ghostMarkers.length > 0 && (
        <div style={{ marginBottom: "14px" }}>
          <p style={{ fontSize: "10px", fontWeight: 700, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: "8px" }}>Marqueurs (aperçu)</p>
          {ghostMarkers.map((m) => (
            <div key={m.id} style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "6px", padding: "8px 10px", background: "#f8fafc", borderRadius: "8px" }}>
              <span style={{ width: "12px", height: "12px", borderRadius: "50%", background: m.type === "competitor" ? "#ef4444" : "#22c55e", flexShrink: 0 }} />
              <span style={{ fontSize: "12px", color: "#374151", fontWeight: 500 }}>{m.tag}</span>
              <span style={{ fontSize: "10px", color: "#94a3b8", marginLeft: "auto" }}>{m.type}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

/* ---- Analytics ---- */
function AnalyticsPanel() {
  const { hexagonRecord, selectedHexIndex, profileId } = useStore();
  const hexs = useMemo(() => Object.values(hexagonRecord), [hexagonRecord]);
  const stats = useMemo(
    () => computeViewportScoreStats(hexs.map((h) => h.score)),
    [hexs],
  );
  const distData = useMemo(
    () => [
      { name: "Excellent (81-100)", value: stats.bins.excellent, color: "#22c55e" },
      { name: "Good (61-80)", value: stats.bins.good, color: "#eab308" },
      { name: "Fair (41-60)", value: stats.bins.fair, color: "#f97316" },
      { name: "Poor (0-40)", value: stats.bins.poor, color: "#ef4444" },
    ],
    [stats],
  );

  const [zone, setZone] = useState<ZoneStatsResponse | null>(null);
  const [zoneLoading, setZoneLoading] = useState(false);
  const [zoneError, setZoneError] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedHexIndex || !profileId) {
      setZone(null);
      setZoneError(null);
      return;
    }
    let cancelled = false;
    setZoneLoading(true);
    setZoneError(null);
    void fetchZoneStats(selectedHexIndex, profileId)
      .then((z) => {
        if (!cancelled) setZone(z);
      })
      .catch(() => {
        if (!cancelled) setZoneError("Impossible de charger les statistiques de zone.");
      })
      .finally(() => {
        if (!cancelled) setZoneLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedHexIndex, profileId]);
  return (
    <div style={{ padding: "0 16px 20px" }}>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px", marginBottom: "16px" }}>
        <div style={{ padding: "14px 12px", background: "#f8fafc", borderRadius: "10px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "4px", marginBottom: "4px" }}>
            <TrendUpIcon color="#1a56db" /><span style={{ fontSize: "10px", color: "#94a3b8" }}>Avg Score</span>
          </div>
          <div style={{ fontSize: "24px", fontWeight: 800, color: "#1e293b" }}>
            {stats.avgScore == null ? "—" : stats.avgScore}
          </div>
          <div style={{ fontSize: "10px", color: "#94a3b8", marginTop: "3px" }}>
            {stats.scored}/{stats.total} cellules scorées
          </div>
        </div>
        <div style={{ padding: "14px 12px", background: "#f8fafc", borderRadius: "10px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "4px", marginBottom: "4px" }}>
            <TrendUpIcon color="#22c55e" /><span style={{ fontSize: "10px", color: "#94a3b8" }}>Viewport</span>
          </div>
          <div style={{ fontSize: "24px", fontWeight: 800, color: "#22c55e" }}>
            {stats.total.toLocaleString("fr-FR")}
          </div>
          <div style={{ fontSize: "10px", color: "#94a3b8", marginTop: "3px" }}>cellules visibles</div>
        </div>
      </div>
      <p style={{ fontSize: "12px", fontWeight: 700, color: "#374151", margin: "16px 0 10px" }}>Score Distribution</p>
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

      <p style={{ fontSize: "12px", fontWeight: 700, color: "#374151", margin: "18px 0 10px" }}>
        Selected Hex — Zone stats
      </p>
      {!selectedHexIndex ? (
        <p style={{ fontSize: "11px", color: "#94a3b8", lineHeight: 1.6 }}>
          Sélectionnez un hexagone pour afficher les statistiques de zone.
        </p>
      ) : !profileId ? (
        <p style={{ fontSize: "11px", color: "#94a3b8", lineHeight: 1.6 }}>
          Sélectionnez un profil commercial pour calculer les statistiques.
        </p>
      ) : zoneLoading ? (
        <p style={{ fontSize: "11px", color: "#94a3b8", lineHeight: 1.6 }}>Chargement…</p>
      ) : zoneError ? (
        <p style={{ fontSize: "11px", color: "#ef4444", lineHeight: 1.6 }}>{zoneError}</p>
      ) : zone ? (
        <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: "10px", padding: "10px 12px" }}>
          <div style={{ fontSize: "11px", color: "#334155", lineHeight: 1.6 }}>
            <div><strong>Population density</strong>: {zone.populationDensity == null ? "—" : Math.round(zone.populationDensity).toLocaleString("fr-FR")}</div>
            <div><strong>Est. daily pedestrians (sim.)</strong>: {zone.estimatedDailyPedestrians == null ? "—" : zone.estimatedDailyPedestrians.toLocaleString("fr-FR")}</div>
          </div>
          <div style={{ marginTop: "10px" }}>
            <div style={{ fontSize: "10px", fontWeight: 800, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: "6px" }}>
              Top POIs
            </div>
            {zone.topPois?.length ? (
              <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                {zone.topPois.slice(0, 8).map((p, i) => (
                  <div key={`${p.typeTag}-${i}`} style={{ fontSize: "11px", color: "#0f172a", lineHeight: 1.35 }}>
                    <strong>{p.name ?? "—"}</strong>
                    <div style={{ color: "#64748b", fontSize: "10px" }}>{p.typeTag}{p.address ? ` · ${p.address}` : ""}</div>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ fontSize: "11px", color: "#94a3b8" }}>Aucun POI trouvé dans cet hex.</div>
            )}
          </div>
        </div>
      ) : (
        <p style={{ fontSize: "11px", color: "#94a3b8", lineHeight: 1.6 }}>—</p>
      )}
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
