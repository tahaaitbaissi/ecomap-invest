import { describe, expect, it } from "vitest";
import {
  MAX_BOUNDS,
  clampPointToPilot,
  clampViewportBbox,
} from "./casablancaMapConstraints";

describe("clampViewportBbox", () => {
  it("intersects viewport with pilot maxBounds", () => {
    const sw = MAX_BOUNDS.getSouthWest();
    const ne = MAX_BOUNDS.getNorthEast();
    const out = clampViewportBbox({
      southWest: { lat: 33.4, lng: -7.7 },
      northEast: { lat: 33.6, lng: -7.4 },
    });
    expect(out.southWest.lat).toBeGreaterThanOrEqual(sw.lat);
    expect(out.southWest.lng).toBeGreaterThanOrEqual(sw.lng);
    expect(out.northEast.lat).toBeLessThanOrEqual(ne.lat);
    expect(out.northEast.lng).toBeLessThanOrEqual(ne.lng);
  });

  it("clamps stray corners outside pilot", () => {
    const sw = MAX_BOUNDS.getSouthWest();
    const ne = MAX_BOUNDS.getNorthEast();
    const out = clampViewportBbox({
      southWest: { lat: -10, lng: -20 },
      northEast: { lat: 50, lng: 10 },
    });
    expect(out).toMatchObject({
      southWest: { lat: sw.lat, lng: sw.lng },
      northEast: { lat: ne.lat, lng: ne.lng },
    });
  });
});

describe("clampPointToPilot", () => {
  it("moves outward points onto pilot edge", () => {
    const ne = MAX_BOUNDS.getNorthEast();
    const [lat, lng] = clampPointToPilot(40, -3);
    expect(lat).toBeLessThanOrEqual(ne.lat);
    expect(lng).toBeLessThanOrEqual(ne.lng);
  });
});
