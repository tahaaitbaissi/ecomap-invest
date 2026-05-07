export type ScoreBinKey = "excellent" | "good" | "fair" | "poor";

export interface ViewportScoreStats {
  total: number;
  scored: number;
  avgScore: number | null;
  bins: Record<ScoreBinKey, number>;
}

export function computeViewportScoreStats(
  scores: Array<number | null | undefined>,
): ViewportScoreStats {
  const bins: Record<ScoreBinKey, number> = {
    excellent: 0,
    good: 0,
    fair: 0,
    poor: 0,
  };

  let scored = 0;
  let sum = 0;

  for (const s of scores) {
    if (s == null || !Number.isFinite(s)) continue;
    scored++;
    sum += s;
    if (s > 80) bins.excellent++;
    else if (s > 60) bins.good++;
    else if (s > 40) bins.fair++;
    else bins.poor++;
  }

  return {
    total: scores.length,
    scored,
    avgScore: scored > 0 ? Math.round(sum / scored) : null,
    bins,
  };
}

