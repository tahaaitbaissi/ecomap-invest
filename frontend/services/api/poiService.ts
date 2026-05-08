import axios from "@/lib/axiosInstance";

export interface PoiDto {
  id: string;
  name: string;
  address: string;
  typeTag: string;
  latitude: number;
  longitude: number;
}

export interface BoundingBox {
  northEast: { lat: number; lng: number };
  southWest: { lat: number; lng: number };
}

export interface FetchPoisOptions {
  includeScore?: boolean;
}

export interface PoiSearchResult {
  id: string;
  name: string;
  typeTag: string;
  lat: number;
  lng: number;
  address: string;
}

export async function searchPois(
  q: string,
  opts?: { limit?: number; signal?: AbortSignal },
): Promise<PoiSearchResult[]> {
  const response = await axios.get<PoiSearchResult[]>("/api/v1/poi/search", {
    params: { q, limit: opts?.limit ?? 10 },
    signal: opts?.signal,
  });
  return response.data ?? [];
}

export async function fetchPoisInBbox(
  bbox: BoundingBox,
  options: FetchPoisOptions = {},
): Promise<PoiDto[]> {
  const { northEast: ne, southWest: sw } = bbox;
  const params = new URLSearchParams({
    minX: String(sw.lng),
    minY: String(sw.lat),
    maxX: String(ne.lng),
    maxY: String(ne.lat),
    includeScore: String(options.includeScore ?? false),
  });
  const response = await axios.get<PoiDto[]>(`/api/v1/poi?${params.toString()}`);
  return response.data;
}
