import axios from "@/lib/axiosInstance";

export interface GeocodingResult {
  displayName: string;
  lat: number;
  lng: number;
}

export interface GeocodingSuggestion {
  displayName: string;
  lat: number;
  lng: number;
  southLat?: number;
  westLng?: number;
  northLat?: number;
  eastLng?: number;
  osmType?: string;
  osmCategory?: string;
}

export async function searchPlaces(
  q: string,
  opts?: { limit?: number; signal?: AbortSignal },
): Promise<GeocodingSuggestion[]> {
  const response = await axios.get<GeocodingSuggestion[]>("/api/v1/geocode/suggest", {
    params: { q, limit: opts?.limit ?? 8 },
    signal: opts?.signal,
  });
  return response.data ?? [];
}

/**
 * Back-compat: first suggestion only (same shape as legacy single-result API).
 * Errors propagate (no silent empty list on network/5xx).
 */
export async function searchAddress(
  query: string,
  opts?: { signal?: AbortSignal },
): Promise<GeocodingResult[]> {
  const rows = await searchPlaces(query.trim(), { limit: 1, signal: opts?.signal });
  if (rows.length === 0) {
    return [];
  }
  const r = rows[0];
  return [{ displayName: r.displayName, lat: r.lat, lng: r.lng }];
}
