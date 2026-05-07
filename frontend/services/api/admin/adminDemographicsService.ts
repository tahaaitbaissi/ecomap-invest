import axios from "@/lib/axiosInstance";

export interface AdminDemographicsResponse {
  h3Index: string;
  populationDensity: number | null;
  avgIncome: number | null;
  lastUpdated: string | null;
}

export interface AdminDemographicsUpsertRequest {
  populationDensity: number;
  avgIncome: number;
}

export async function adminGetDemographics(h3Index: string): Promise<AdminDemographicsResponse> {
  const res = await axios.get<AdminDemographicsResponse>(`/api/v1/admin/demographics/${h3Index}`);
  return res.data;
}

export async function adminUpsertDemographics(
  h3Index: string,
  payload: AdminDemographicsUpsertRequest,
): Promise<AdminDemographicsResponse> {
  const res = await axios.put<AdminDemographicsResponse>(
    `/api/v1/admin/demographics/${h3Index}`,
    payload,
  );
  return res.data;
}

export async function adminDeleteDemographics(h3Index: string): Promise<void> {
  await axios.delete(`/api/v1/admin/demographics/${h3Index}`);
}

