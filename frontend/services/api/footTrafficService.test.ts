import { beforeEach, describe, expect, it, vi } from "vitest";
import axios from "@/lib/axiosInstance";
import { getFootTrafficHourly } from "./footTrafficService";

vi.mock("@/lib/axiosInstance", () => ({
  default: {
    get: vi.fn(),
  },
}));

describe("footTrafficService", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
    vi.mocked(axios.get).mockResolvedValue({ data: {} });
  });

  it("builds hourly URL with query params", async () => {
    await getFootTrafficHourly({ h3Index: "h", day: "WD", month: 5 });
    expect(axios.get).toHaveBeenCalledWith("/api/v1/foot-traffic/hourly/h?day=WD&month=5");
  });
});

