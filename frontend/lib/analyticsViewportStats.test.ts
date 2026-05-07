import { describe, expect, it } from "vitest";
import { computeViewportScoreStats } from "./analyticsViewportStats";

describe("computeViewportScoreStats", () => {
  it("computes avg and bins from scores", () => {
    const out = computeViewportScoreStats([90, 70, 50, 10, null, undefined]);
    expect(out.total).toBe(6);
    expect(out.scored).toBe(4);
    expect(out.avgScore).toBe(Math.round((90 + 70 + 50 + 10) / 4));
    expect(out.bins).toEqual({
      excellent: 1,
      good: 1,
      fair: 1,
      poor: 1,
    });
  });

  it("handles empty or unscored viewport", () => {
    const out = computeViewportScoreStats([null, undefined]);
    expect(out.scored).toBe(0);
    expect(out.avgScore).toBeNull();
    expect(out.bins.excellent + out.bins.good + out.bins.fair + out.bins.poor).toBe(0);
  });
});

