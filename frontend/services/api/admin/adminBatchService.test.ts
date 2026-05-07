import { beforeEach, describe, expect, it, vi } from "vitest";
import axios from "@/lib/axiosInstance";
import { adminBatchStatus, adminTriggerBatch } from "./adminBatchService";

vi.mock("@/lib/axiosInstance", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe("adminBatchService", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
    vi.mocked(axios.post).mockReset();
    vi.mocked(axios.get).mockResolvedValue({ data: { status: "COMPLETED" } });
    vi.mocked(axios.post).mockResolvedValue({ data: { executionId: 1, status: "STARTED" } });
  });

  it("triggers job", async () => {
    await adminTriggerBatch();
    expect(axios.post).toHaveBeenCalledWith("/api/v1/admin/batch/trigger");
  });

  it("polls status", async () => {
    await adminBatchStatus(99);
    expect(axios.get).toHaveBeenCalledWith("/api/v1/admin/batch/status/99");
  });
});

