import { beforeEach, describe, expect, it, vi } from "vitest";
import axios from "@/lib/axiosInstance";
import { fetchPoisInBbox } from "./poiService";

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
      expect.stringMatching(/\/api\/v1\/poi\?minX=0&minY=0&maxX=2&maxY=1/),
    );
  });
});
