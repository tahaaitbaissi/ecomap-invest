import { beforeEach, describe, expect, it, vi } from "vitest";
import axios from "@/lib/axiosInstance";
import { adminDeleteDemographics, adminGetDemographics, adminUpsertDemographics } from "./adminDemographicsService";

vi.mock("@/lib/axiosInstance", () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("adminDemographicsService", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
    vi.mocked(axios.put).mockReset();
    vi.mocked(axios.delete).mockReset();
    vi.mocked(axios.get).mockResolvedValue({ data: { h3Index: "h3" } });
    vi.mocked(axios.put).mockResolvedValue({ data: { h3Index: "h3" } });
    vi.mocked(axios.delete).mockResolvedValue({});
  });

  it("wraps get/upsert/delete", async () => {
    await adminGetDemographics("h3");
    expect(axios.get).toHaveBeenCalledWith("/api/v1/admin/demographics/h3");

    await adminUpsertDemographics("h3", { populationDensity: 1, avgIncome: 2 });
    expect(axios.put).toHaveBeenCalledWith("/api/v1/admin/demographics/h3", {
      populationDensity: 1,
      avgIncome: 2,
    });

    await adminDeleteDemographics("h3");
    expect(axios.delete).toHaveBeenCalledWith("/api/v1/admin/demographics/h3");
  });
});

