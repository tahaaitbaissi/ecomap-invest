function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t;
}

function lerpColor(c1: string, c2: string, t: number): string {
  const p = (hex: string, i: number) => parseInt(hex.slice(i, i + 2), 16);
  const r = Math.round(lerp(p(c1, 1), p(c2, 1), t));
  const g = Math.round(lerp(p(c1, 3), p(c2, 3), t));
  const b = Math.round(lerp(p(c1, 5), p(c2, 5), t));
  return `#${r.toString(16).padStart(2, "0")}${g.toString(16).padStart(2, "0")}${b.toString(16).padStart(2, "0")}`;
}

export function getHexColor(score: number): string {
  const s = Math.max(0, Math.min(100, score));
  if (s < 30) return lerpColor("#FF2222", "#FF8800", s / 29);
  if (s <= 60) return lerpColor("#FFA500", "#FFEE00", (s - 30) / 30);
  return lerpColor("#AEDD00", "#00CC44", (s - 61) / 39);
}

export function getHexOpacity(score: number): number {
  return 0.4 + (score / 100) * 0.35;
}
