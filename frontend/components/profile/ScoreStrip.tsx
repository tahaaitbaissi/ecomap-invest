"use client";

import { useMemo } from "react";
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from "recharts";
import { useStore } from "@/store/useStore";

export default function ScoreStrip() {
  const hexagonRecord = useStore((s) => s.hexagonRecord);
  const hexs = useMemo(() => Object.values(hexagonRecord), [hexagonRecord]);

  const data = useMemo(() => {
    const buckets = Array.from({ length: 10 }, (_, i) => ({
      range: `${i * 10}-${i * 10 + 9}`,
      count: 0,
    }));
    hexs.forEach((h) => {
      const idx = Math.min(9, Math.floor(h.score / 10));
      buckets[idx].count++;
    });
    return buckets;
  }, [hexs]);

  if (hexs.length === 0) return null;

  const avg = Math.round(hexs.reduce((s, h) => s + h.score, 0) / hexs.length);

  return (
    <div style={{ background: "#fff", borderRadius: "14px", border: "1px solid #e2e8f0", padding: "10px 16px", marginBottom: "8px", display: "flex", alignItems: "center", gap: "16px" }}>
      <div style={{ flexShrink: 0 }}>
        <div style={{ fontSize: "10px", color: "#94a3b8", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.06em" }}>Score moyen</div>
        <div style={{ fontSize: "22px", fontWeight: 800, color: "#1e293b", lineHeight: 1.2 }}>{avg}</div>
        <div style={{ fontSize: "10px", color: "#94a3b8" }}>{hexs.length} zones</div>
      </div>
      <div style={{ flex: 1, height: "50px" }}>
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 4, right: 0, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id="scoreGrad" x1="0" y1="0" x2="1" y2="0">
                <stop offset="0%" stopColor="#ef4444" />
                <stop offset="50%" stopColor="#eab308" />
                <stop offset="100%" stopColor="#22c55e" />
              </linearGradient>
            </defs>
            <XAxis dataKey="range" hide />
            <YAxis hide />
            <Tooltip
              contentStyle={{ fontSize: "10px", borderRadius: "8px", border: "none", boxShadow: "0 4px 12px rgba(0,0,0,0.1)" }}
              formatter={(v) => [v ?? 0, "hexagones"]}
            />
            <Area
              type="monotone"
              dataKey="count"
              stroke="url(#scoreGrad)"
              fill="url(#scoreGrad)"
              fillOpacity={0.25}
              strokeWidth={2}
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
