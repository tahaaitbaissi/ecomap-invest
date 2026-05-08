import { beforeEach, describe, expect, it, vi } from "vitest";
import axios from "@/lib/axiosInstance";
import { searchPlaces, searchAddress } from "./geocodingService";

vi.mock("@/lib/axiosInstance", () => ({
  default: {
    get: vi.fn(),
  },
}));

describe("searchPlaces", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
  });

  it("calls GET /api/v1/geocode/suggest with q and limit", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: [{ displayName: "X", lat: 1, lng: 2 }],
    });
    const out = await searchPlaces("Maarif", { limit: 5 });
    expect(axios.get).toHaveBeenCalledWith("/api/v1/geocode/suggest", {
      params: { q: "Maarif", limit: 5 },
      signal: undefined,
    });
    expect(out).toHaveLength(1);
    expect(out[0].displayName).toBe("X");
  });

  it("forwards AbortSignal", async () => {
    const ac = new AbortController();
    vi.mocked(axios.get).mockResolvedValue({ data: [] });
    await searchPlaces("a", { signal: ac.signal });
    expect(axios.get).toHaveBeenCalledWith(
      "/api/v1/geocode/suggest",
      expect.objectContaining({ signal: ac.signal }),
    );
  });
});

describe("searchAddress", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
  });

  it("returns first suggestion as GeocodingResult list", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: [{ displayName: "A", lat: 3, lng: 4 }],
    });
    const out = await searchAddress("q");
    expect(out).toEqual([{ displayName: "A", lat: 3, lng: 4 }]);
  });

  it("returns empty array when suggest is empty", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: [] });
    const out = await searchAddress("none");
    expect(out).toEqual([]);
  });
});
