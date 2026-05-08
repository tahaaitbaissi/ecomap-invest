import { beforeEach, describe, expect, it, vi } from "vitest";
import axios from "@/lib/axiosInstance";
import { getHexagonByIndex } from "./hexagonService";

vi.mock("@/lib/axiosInstance", () => ({
  default: {
    get: vi.fn(),
  },
}));

describe("getHexagonByIndex", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
  });

  it("GET encodes path and returns hex DTO", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: { h3Index: "891ea6c0d47ffff", score: null, boundary: [{ lat: 1, lng: 2 }] },
    });
    const h = await getHexagonByIndex("891ea6c0d47ffff");
    expect(axios.get).toHaveBeenCalledWith("/api/v1/hexagons/h3/891ea6c0d47ffff", {
      signal: undefined,
    });
    expect(h.h3Index).toBe("891ea6c0d47ffff");
  });

  it("passes AbortSignal", async () => {
    const ac = new AbortController();
    vi.mocked(axios.get).mockResolvedValue({ data: { h3Index: "x", score: null, boundary: [] } });
    await getHexagonByIndex("x", ac.signal);
    expect(axios.get).toHaveBeenCalledWith(expect.any(String), { signal: ac.signal });
  });
});
