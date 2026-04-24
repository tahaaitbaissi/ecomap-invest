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

  it("calls GET with swLng,swLat,neLng,neLat bbox string", async () => {
    await fetchPoisInBbox({
      northEast: { lat: 1, lng: 2 },
      southWest: { lat: 0, lng: 0 },
    });
    expect(axios.get).toHaveBeenCalledWith(
      expect.stringMatching(/\/api\/v1\/poi\?bbox=0,0,2,1/),
    );
  });
});
