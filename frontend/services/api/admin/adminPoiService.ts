import axios from "@/lib/axiosInstance";

export interface AdminPoiResponse {
  id: string;
  osmId: string;
  name: string | null;
  address: string | null;
  typeTag: string;
  lat: number;
  lng: number;
  priceLevel: number | null;
  rating: number | null;
  importedAt: string | null;
}

export interface AdminPoiSearchResponse {
  items: AdminPoiResponse[];
  totalElements: number;
  page: number;
  size: number;
}

export interface AdminPoiUpsertRequest {
  osmId?: string;
  name?: string;
  address?: string;
  typeTag: string;
  lat: number;
  lng: number;
  priceLevel?: number | null;
  rating?: number | null;
}

export async function adminSearchPois(params: {
  minX?: number;
  minY?: number;
  maxX?: number;
  maxY?: number;
  typeTag?: string;
  nameLike?: string;
  page?: number;
  size?: number;
  sort?: string;
}): Promise<AdminPoiSearchResponse> {
  const res = await axios.get<AdminPoiSearchResponse>("/api/v1/admin/poi/search", { params });
  return res.data;
}

export async function adminCreatePoi(payload: AdminPoiUpsertRequest): Promise<AdminPoiResponse> {
  const res = await axios.post<AdminPoiResponse>("/api/v1/admin/poi", payload);
  return res.data;
}

export async function adminUpdatePoi(id: string, payload: AdminPoiUpsertRequest): Promise<AdminPoiResponse> {
  const res = await axios.patch<AdminPoiResponse>(`/api/v1/admin/poi/${id}`, payload);
  return res.data;
}

export async function adminDeletePoi(id: string): Promise<void> {
  await axios.delete(`/api/v1/admin/poi/${id}`);
}

