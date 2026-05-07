import { beforeEach, describe, expect, it, vi } from "vitest";
import axios from "@/lib/axiosInstance";
import {
  archiveDynamicProfile,
  duplicateDynamicProfile,
  fetchMyProfiles,
  fetchProfileTags,
  generateDynamicProfile,
  updateDynamicProfile,
} from "./profileService";

vi.mock("@/lib/axiosInstance", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("profileService", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
    vi.mocked(axios.post).mockReset();
    vi.mocked(axios.patch).mockReset();
    vi.mocked(axios.delete).mockReset();
    vi.mocked(axios.get).mockResolvedValue({ data: { content: [] } });
    vi.mocked(axios.post).mockResolvedValue({ data: { id: "p1" } });
    vi.mocked(axios.patch).mockResolvedValue({ data: { id: "p1" } });
    vi.mocked(axios.delete).mockResolvedValue({});
  });

  it("lists profiles with archived filter", async () => {
    await fetchMyProfiles(1, 25, true);
    expect(axios.get).toHaveBeenCalledWith("/api/v1/profile/my", {
      params: {
        page: 1,
        size: 25,
        sort: "generatedAt,desc",
        includeArchived: true,
      },
    });
  });

  it("wraps generate, update, duplicate, and archive calls", async () => {
    await generateDynamicProfile("Snack");
    expect(axios.post).toHaveBeenCalledWith("/api/v1/profile/generate", { query: "Snack" });

    await updateDynamicProfile("p1", { name: "Snack profile" });
    expect(axios.patch).toHaveBeenCalledWith("/api/v1/profile/p1", { name: "Snack profile" });

    await duplicateDynamicProfile("p1");
    expect(axios.post).toHaveBeenCalledWith("/api/v1/profile/p1/duplicate");

    await archiveDynamicProfile("p1");
    expect(axios.delete).toHaveBeenCalledWith("/api/v1/profile/p1");
  });

  it("fetches supported profile tags", async () => {
    vi.mocked(axios.get).mockResolvedValueOnce({ data: [{ tag: "amenity=cafe" }] });
    const tags = await fetchProfileTags();
    expect(axios.get).toHaveBeenCalledWith("/api/v1/profile/tags");
    expect(tags).toEqual([{ tag: "amenity=cafe" }]);
  });
});
