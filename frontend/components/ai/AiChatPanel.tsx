"use client";

import { useEffect, useMemo, useState } from "react";
import { useStore } from "@/store/useStore";
import { fetchHexDetails, type HexExplanationContextDto } from "@/services/api/profileService";
import { useSSEChat } from "@/hooks/useSSEChat";

function summarizeCtx(ctx: HexExplanationContextDto): string {
  const topDrivers = [...(ctx.drivers ?? [])]
    .filter((d) => d.countInsideAcrossLeaves > 0)
    .sort((a, b) => b.weightedContributionAcrossLeaves - a.weightedContributionAcrossLeaves)
    .slice(0, 4)
    .map(
      (d) =>
        `- **${d.tag}**: ${d.countInsideAcrossLeaves} × ${d.weight.toFixed(2)} → ${d.weightedContributionAcrossLeaves.toFixed(2)}`,
    )
    .join("\n");

  const compLine =
    ctx.totalCompetitorPoisUnweightedAcrossLeaves != null
      ? `- **Concurrents (non pondéré)**: Σ ${ctx.totalCompetitorPoisUnweightedAcrossLeaves} POI`
      : `- **Concurrents**: —`;

  const demoLine =
    ctx.populationDensityAvg != null
      ? `- **Densité**: ~${Math.round(ctx.populationDensityAvg).toLocaleString("fr-FR")}`
      : `- **Démographie**: —`;

  return (
    `**XAI brief (faits déterministes)**\n\n` +
    `- **Profil**: ${ctx.profileName}\n` +
    `- **Score affiché**: ${ctx.computedDisplayScore == null ? "—" : Math.round(ctx.computedDisplayScore)}/100\n` +
    `- **Brut moyen**: ${ctx.averageRawAcrossLeaves.toFixed(4)} (stretch [${ctx.normalizationStretchLow.toFixed(3)}, ${ctx.normalizationStretchHigh.toFixed(3)}]${ctx.normalizationFlat ? ", plat" : ""})\n` +
    `${compLine}\n` +
    `${demoLine}\n\n` +
    `**Top conducteurs**\n` +
    (topDrivers.length > 0 ? `${topDrivers}\n` : `- —\n`)
  );
}

function ContextDisclosure({ ctx }: { ctx: HexExplanationContextDto | null }) {
  const [open, setOpen] = useState(true);
  if (!ctx) return null;
  return (
    <div style={{ marginBottom: "10px" }}>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        style={{
          width: "100%",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: "10px 12px",
          borderRadius: "10px",
          border: "1px solid #e2e8f0",
          background: "#fff",
          cursor: "pointer",
        }}
      >
        <span style={{ fontSize: "12px", fontWeight: 800, color: "#0f172a" }}>Contexte (faits)</span>
        <span style={{ fontSize: "12px", color: "#64748b" }}>{open ? "▾" : "▸"}</span>
      </button>
      {open && (
        <div
          style={{
            marginTop: "8px",
            background: "#f8fafc",
            border: "1px solid #e2e8f0",
            borderRadius: "10px",
            padding: "10px 12px",
            fontSize: "11px",
            color: "#334155",
            lineHeight: 1.6,
          }}
        >
          <div>
            <strong>Hex</strong> {ctx.h3Index.slice(-8)} · {ctx.aggregatedFromGridLeaves ? `Σ ${ctx.gridLeafCount} @${ctx.gridLeafResolution}` : `@${ctx.h3InputResolution}`}
          </div>
          <div>
            <strong>Stretch</strong> [{ctx.normalizationStretchLow.toFixed(3)}, {ctx.normalizationStretchHigh.toFixed(3)}]
            {ctx.normalizationFlat ? " (plat)" : ""}
          </div>
          <div>
            <strong>Drivers</strong> {ctx.drivers?.filter((d) => d.countInsideAcrossLeaves > 0).length ?? 0} tags ·{" "}
            <strong>Competitors</strong> Σ {ctx.totalCompetitorPoisUnweightedAcrossLeaves ?? 0}
          </div>
        </div>
      )}
    </div>
  );
}

function Bubble({
  role,
  content,
}: {
  role: "system" | "user" | "assistant";
  content: string;
}) {
  const isUser = role === "user";
  const isSystem = role === "system";
  const bg = isSystem ? "#fff7ed" : isUser ? "#1a56db" : "#f8fafc";
  const color = isSystem ? "#9a3412" : isUser ? "#ffffff" : "#0f172a";
  const border = isSystem ? "1px solid #fed7aa" : isUser ? "none" : "1px solid #e2e8f0";
  return (
    <div style={{ display: "flex", justifyContent: isUser ? "flex-end" : "flex-start" }}>
      <div
        style={{
          maxWidth: "100%",
          width: "fit-content",
          borderRadius: "12px",
          borderTopLeftRadius: isUser ? "12px" : "4px",
          borderTopRightRadius: isUser ? "4px" : "12px",
          padding: "10px 12px",
          background: bg,
          border,
          color,
          fontSize: "12px",
          lineHeight: 1.6,
          whiteSpace: "pre-wrap",
        }}
      >
        {content}
      </div>
    </div>
  );
}

export default function AiChatPanel() {
  const { selectedHexIndex, hexagonRecord, profileId } = useStore();
  const viewportCellCount = Object.keys(hexagonRecord).length;
  const conversationId = profileId; // profile-scoped per plan

  const [ctx, setCtx] = useState<HexExplanationContextDto | null>(null);
  const [ctxLoading, setCtxLoading] = useState(false);
  const [ctxError, setCtxError] = useState<string | null>(null);

  const hex = selectedHexIndex ? hexagonRecord[selectedHexIndex] : null;
  const hexLabel = useMemo(() => (hex ? `Hex #${parseInt(hex.h3Index.slice(-3), 16) % 100}` : null), [hex]);

  const chat = useSSEChat({
    conversationId,
    profileId,
    selectedHexIndex,
    viewportCellCount: viewportCellCount > 0 ? viewportCellCount : undefined,
  });

  useEffect(() => {
    if (!profileId || !selectedHexIndex) {
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
      .catch(() => {
        if (!cancelled) setCtxError("Impossible de charger les faits XAI.");
      })
      .finally(() => {
        if (!cancelled) setCtxLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [profileId, selectedHexIndex, viewportCellCount]);

  // On hex change: inject deterministic brief, keep conversation going.
  useEffect(() => {
    if (!selectedHexIndex || !profileId) return;
    if (!ctx) return;
    chat.injectSystemMessage(`Contexte changé: ${hexLabel ?? selectedHexIndex.slice(-8)}`);
    chat.injectAssistantMessage(summarizeCtx(ctx));
    // We intentionally do NOT auto-call the LLM here; follow-ups will stream.
    // This keeps the first message deterministic as requested.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedHexIndex, profileId, ctx]);

  const [input, setInput] = useState("");

  return (
    <div style={{ display: "flex", flexDirection: "column", padding: "0 16px 16px", height: "calc(100vh - 150px)" }}>
      <ContextDisclosure ctx={ctx} />

      <div style={{ flex: 1, overflowY: "auto", marginBottom: "12px", display: "flex", flexDirection: "column", gap: "10px" }}>
        {!selectedHexIndex ? (
          <p style={{ fontSize: "12px", color: "#94a3b8", textAlign: "center", marginTop: "24px", lineHeight: 1.7 }}>
            Sélectionnez un hexagone<br />pour démarrer l’assistant
          </p>
        ) : !profileId ? (
          <p style={{ fontSize: "12px", color: "#94a3b8", textAlign: "center", marginTop: "24px", lineHeight: 1.7 }}>
            Sélectionnez un profil commercial<br />pour utiliser l’assistant
          </p>
        ) : (
          <>
            {ctxLoading && (
              <Bubble role="system" content="Chargement du contexte XAI…" />
            )}
            {ctxError && <Bubble role="system" content={ctxError} />}
            {chat.messages.length === 0 && ctx && (
              <Bubble role="assistant" content={summarizeCtx(ctx)} />
            )}
            {chat.messages.map((m) => (
              <Bubble key={m.id} role={m.role} content={m.content} />
            ))}
            {chat.error && <Bubble role="system" content={chat.error} />}
          </>
        )}
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          const msg = input.trim();
          if (!msg) return;
          chat.send(msg);
          setInput("");
        }}
        style={{ display: "flex", gap: "8px", flexShrink: 0 }}
      >
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder={chat.isStreaming ? "Réponse en cours…" : "Posez une question (opportunité vs heatmap, drivers, concurrence…)"}
          disabled={!profileId || !selectedHexIndex || chat.isStreaming}
          style={{
            flex: 1,
            padding: "10px 14px",
            borderRadius: "10px",
            border: "1.5px solid #e2e8f0",
            fontSize: "12px",
            outline: "none",
            color: "#374151",
            background: !profileId || !selectedHexIndex ? "#f8fafc" : "#fff",
          }}
        />
        {chat.isStreaming ? (
          <button
            type="button"
            onClick={chat.stop}
            style={{
              width: "38px",
              height: "38px",
              borderRadius: "10px",
              background: "#ffffff",
              border: "1px solid #ef4444",
              color: "#ef4444",
              cursor: "pointer",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              flexShrink: 0,
            }}
            title="Stop"
          >
            ■
          </button>
        ) : (
          <button
            type="submit"
            style={{
              width: "38px",
              height: "38px",
              borderRadius: "10px",
              background: "#1a56db",
              border: "none",
              color: "#fff",
              cursor: "pointer",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              flexShrink: 0,
              opacity: !profileId || !selectedHexIndex ? 0.5 : 1,
            }}
            disabled={!profileId || !selectedHexIndex}
            title="Send"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <line x1="22" y1="2" x2="11" y2="13" />
              <polygon points="22 2 15 22 11 13 2 9 22 2" />
            </svg>
          </button>
        )}
      </form>
    </div>
  );
}

