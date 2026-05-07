import { beforeEach, describe, expect, it, vi } from "vitest";
import axios from "@/lib/axiosInstance";
import { adminListUsers, adminSetUserRole } from "./adminUsersService";

vi.mock("@/lib/axiosInstance", () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
  },
}));

describe("adminUsersService", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
    vi.mocked(axios.patch).mockReset();
    vi.mocked(axios.get).mockResolvedValue({ data: { items: [], totalElements: 0, page: 0, size: 25 } });
    vi.mocked(axios.patch).mockResolvedValue({ data: { id: "u1" } });
  });

  it("lists users with pagination params", async () => {
    await adminListUsers({ page: 1, size: 10 });
    expect(axios.get).toHaveBeenCalledWith("/api/v1/admin/users", { params: { page: 1, size: 10 } });
  });

  it("supports emailLike filter", async () => {
    await adminListUsers({ page: 0, size: 25, emailLike: "admin" });
    expect(axios.get).toHaveBeenCalledWith("/api/v1/admin/users", {
      params: { page: 0, size: 25, emailLike: "admin" },
    });
  });

  it("sets role", async () => {
    await adminSetUserRole("u1", "ROLE_ADMIN");
    expect(axios.patch).toHaveBeenCalledWith("/api/v1/admin/users/u1/role", { role: "ROLE_ADMIN" });
  });
});

