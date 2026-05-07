import { beforeEach, describe, expect, it, vi } from "vitest";
import axios from "@/lib/axiosInstance";
import { adminListAuditLogs } from "./adminAuditService";

vi.mock("@/lib/axiosInstance", () => ({
  default: {
    get: vi.fn(),
  },
}));

describe("adminAuditService", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
    vi.mocked(axios.get).mockResolvedValue({ data: { items: [], totalElements: 0, page: 0, size: 50 } });
  });

  it("lists logs with filters", async () => {
    await adminListAuditLogs({ userEmail: "a", action: "X", success: true, page: 1, size: 10 });
    expect(axios.get).toHaveBeenCalledWith("/api/v1/admin/audit", {
      params: { userEmail: "a", action: "X", success: true, page: 1, size: 10 },
    });
  });
});

