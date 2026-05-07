import { beforeEach, describe, expect, it, vi } from "vitest";
import axios from "@/lib/axiosInstance";
import { fetchZoneStats } from "./analyticsService";

vi.mock("@/lib/axiosInstance", () => ({
  default: {
    get: vi.fn(),
  },
}));

describe("analyticsService", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
    vi.mocked(axios.get).mockResolvedValue({ data: { h3Index: "h", profileId: "p" } });
  });

  it("fetches zone stats with params", async () => {
    await fetchZoneStats("89283082803ffff", "p1");
    expect(axios.get).toHaveBeenCalledWith("/api/v1/analytics/zone", {
      params: { h3Index: "89283082803ffff", profileId: "p1" },
    });
  });
});

