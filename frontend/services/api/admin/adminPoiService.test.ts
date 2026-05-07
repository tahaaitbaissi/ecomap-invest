import { beforeEach, describe, expect, it, vi } from "vitest";
import axios from "@/lib/axiosInstance";
import { adminCreatePoi, adminDeletePoi, adminSearchPois, adminUpdatePoi } from "./adminPoiService";

vi.mock("@/lib/axiosInstance", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("adminPoiService", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
    vi.mocked(axios.post).mockReset();
    vi.mocked(axios.patch).mockReset();
    vi.mocked(axios.delete).mockReset();
    vi.mocked(axios.get).mockResolvedValue({ data: { items: [], totalElements: 0, page: 0, size: 25 } });
    vi.mocked(axios.post).mockResolvedValue({ data: { id: "p1" } });
    vi.mocked(axios.patch).mockResolvedValue({ data: { id: "p1" } });
    vi.mocked(axios.delete).mockResolvedValue({});
  });

  it("searches via GET params", async () => {
    await adminSearchPois({ typeTag: "category=office", page: 1, size: 10, sort: "name,asc" });
    expect(axios.get).toHaveBeenCalledWith("/api/v1/admin/poi/search", {
      params: { typeTag: "category=office", page: 1, size: 10, sort: "name,asc" },
    });
  });

  it("wraps create/update/delete", async () => {
    await adminCreatePoi({ typeTag: "category=office", lat: 1, lng: 2 });
    expect(axios.post).toHaveBeenCalledWith("/api/v1/admin/poi", {
      typeTag: "category=office",
      lat: 1,
      lng: 2,
    });

    await adminUpdatePoi("id1", { typeTag: "category=office", lat: 1, lng: 2 });
    expect(axios.patch).toHaveBeenCalledWith("/api/v1/admin/poi/id1", {
      typeTag: "category=office",
      lat: 1,
      lng: 2,
    });

    await adminDeletePoi("id1");
    expect(axios.delete).toHaveBeenCalledWith("/api/v1/admin/poi/id1");
  });
});

