import { beforeEach, describe, expect, it, vi } from "vitest";
import axios from "@/lib/axiosInstance";
import { fetchPoisInBbox, searchPois } from "./poiService";

vi.mock("@/lib/axiosInstance", () => ({
  default: {
    get: vi.fn(),
  },
}));

describe("fetchPoisInBbox", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
    vi.mocked(axios.get).mockResolvedValue({ data: [] });
  });

  it("calls GET with minX,minY,maxX,maxY viewport params", async () => {
    await fetchPoisInBbox({
      northEast: { lat: 1, lng: 2 },
      southWest: { lat: 0, lng: 0 },
    });
    expect(axios.get).toHaveBeenCalledWith(
      expect.stringMatching(/\/api\/v1\/poi\?minX=0&minY=0&maxX=2&maxY=1&includeScore=false/),
    );
  });

  it("can request saturation scores when needed", async () => {
    await fetchPoisInBbox(
      {
        northEast: { lat: 1, lng: 2 },
        southWest: { lat: 0, lng: 0 },
      },
      { includeScore: true },
    );
    expect(axios.get).toHaveBeenCalledWith(
      expect.stringMatching(/includeScore=true/),
    );
  });
});

describe("searchPois", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
    vi.mocked(axios.get).mockResolvedValue({ data: [] });
  });

  it("calls GET /api/v1/poi/search with q and limit", async () => {
    await searchPois("Carrefour", { limit: 5 });
    expect(axios.get).toHaveBeenCalledWith("/api/v1/poi/search", {
      params: { q: "Carrefour", limit: 5 },
      signal: undefined,
    });
  });
});
