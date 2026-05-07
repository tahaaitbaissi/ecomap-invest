import { describe, expect, it } from "vitest";
import { h3ResolutionForZoom } from "./mapHexResolution";

describe("h3ResolutionForZoom", () => {
  it("forces res 9 in simulation mode", () => {
    expect(h3ResolutionForZoom(10, true)).toBe(9);
    expect(h3ResolutionForZoom(16, true)).toBe(9);
  });

  it("maps zoom bands when not simulating", () => {
    expect(h3ResolutionForZoom(10, false)).toBe(7);
    expect(h3ResolutionForZoom(11, false)).toBe(7);
    expect(h3ResolutionForZoom(12, false)).toBe(8);
    expect(h3ResolutionForZoom(13, false)).toBe(8);
    expect(h3ResolutionForZoom(14, false)).toBe(9);
    expect(h3ResolutionForZoom(18, false)).toBe(9);
  });
});
