import { beforeEach, describe, expect, it, vi } from "vitest";
import axios from "@/lib/axiosInstance";
import {
  adminAuditFootTraffic,
  adminGetFootTrafficParams,
  adminListFootTrafficParams,
  adminRecomputeFootTraffic,
  adminUpsertFootTrafficParams,
} from "./adminFootTrafficService";

vi.mock("@/lib/axiosInstance", () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
    post: vi.fn(),
  },
}));

describe("adminFootTrafficService", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
    vi.mocked(axios.put).mockReset();
    vi.mocked(axios.post).mockReset();
    vi.mocked(axios.get).mockResolvedValue({ data: {} });
    vi.mocked(axios.put).mockResolvedValue({ data: {} });
    vi.mocked(axios.post).mockResolvedValue({ data: {} });
  });

  it("wraps endpoints", async () => {
    await adminListFootTrafficParams();
    expect(axios.get).toHaveBeenCalledWith("/api/v1/admin/foot-traffic/params");

    await adminGetFootTrafficParams("CBD");
    expect(axios.get).toHaveBeenCalledWith("/api/v1/admin/foot-traffic/params/CBD");

    await adminUpsertFootTrafficParams("CBD", {
      baseDailyMin: 1,
      baseDailyMax: 2,
      poiDensityCap: 10,
      popDensityCap: 1000,
      incomeWeight: 0,
      hourlyCurveWd: Array(24).fill(1),
      hourlyCurveSat: Array(24).fill(1),
      hourlyCurveSun: Array(24).fill(1),
      seasonalScalers: Array(12).fill(1),
      dayScalerSat: 1,
      dayScalerSun: 1,
      noiseSigma: 0.1,
    });
    expect(axios.put).toHaveBeenCalled();

    await adminRecomputeFootTraffic({ scenarioId: 0, h3Prefix: "8923" });
    expect(axios.post).toHaveBeenCalledWith("/api/v1/admin/foot-traffic/recompute?scenarioId=0&h3Prefix=8923");

    await adminAuditFootTraffic("h");
    expect(axios.get).toHaveBeenCalledWith("/api/v1/admin/foot-traffic/audit/h");
  });
});

