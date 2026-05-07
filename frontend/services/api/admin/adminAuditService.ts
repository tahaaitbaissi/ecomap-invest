import axios from "@/lib/axiosInstance";

export interface AdminAuditLogItemResponse {
  id: number;
  auditId: string;
  userId: string | null;
  occurredAt: string | null;
  userEmail: string | null;
  action: string | null;
  method: string | null;
  argsSummary: string | null;
  durationMs: number | null;
  success: boolean | null;
  errorClass: string | null;
  errorMessage: string | null;
}

export interface AdminAuditLogListResponse {
  items: AdminAuditLogItemResponse[];
  totalElements: number;
  page: number;
  size: number;
}

export async function adminListAuditLogs(params: {
  userEmail?: string;
  action?: string;
  success?: boolean;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}): Promise<AdminAuditLogListResponse> {
  const res = await axios.get<AdminAuditLogListResponse>("/api/v1/admin/audit", { params });
  return res.data;
}

