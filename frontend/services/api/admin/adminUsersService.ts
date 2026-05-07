import axios from "@/lib/axiosInstance";

export interface AdminUserListItemResponse {
  id: string;
  email: string;
  companyName: string | null;
  role: string;
  createdAt: string | null;
}

export interface AdminUserListResponse {
  items: AdminUserListItemResponse[];
  totalElements: number;
  page: number;
  size: number;
}

export async function adminListUsers(params?: {
  page?: number;
  size?: number;
  emailLike?: string;
}): Promise<AdminUserListResponse> {
  const page = params?.page ?? 0;
  const size = params?.size ?? 25;
  const emailLike = params?.emailLike?.trim() ? params.emailLike.trim() : undefined;
  const res = await axios.get<AdminUserListResponse>("/api/v1/admin/users", {
    params: { page, size, ...(emailLike ? { emailLike } : {}) },
  });
  return res.data;
}

export async function adminSetUserRole(
  id: string,
  role: "ROLE_INVESTOR" | "ROLE_ADMIN",
): Promise<AdminUserListItemResponse> {
  const res = await axios.patch<AdminUserListItemResponse>(`/api/v1/admin/users/${id}/role`, {
    role,
  });
  return res.data;
}

