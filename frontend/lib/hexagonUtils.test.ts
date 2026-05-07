import { describe, expect, it } from "vitest";
import { getHexOpacity } from "./hexagonUtils";

describe("hexagonUtils", () => {
  it("clamps opacity for out-of-range scores", () => {
    expect(getHexOpacity(-100)).toBeCloseTo(0.4, 6);
    expect(getHexOpacity(0)).toBeCloseTo(0.4, 6);
    expect(getHexOpacity(100)).toBeCloseTo(0.75, 6);
    // above 100 should clamp to same as 100
    expect(getHexOpacity(1000)).toBeCloseTo(getHexOpacity(100), 6);
  });
});

