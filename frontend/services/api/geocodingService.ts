import axios from "@/lib/axiosInstance";

export interface GeocodingResult {
  displayName: string;
  lat: number;
  lng: number;
}

/**
 * Backend returns a single optional result (404 when not found).
 * UI expects a list for dropdown-style search.
 */
export async function searchAddress(query: string): Promise<GeocodingResult[]> {
  try {
    const response = await axios.get<GeocodingResult>("/api/v1/geocode", {
      params: { q: query },
      validateStatus: (s) => (s >= 200 && s < 300) || s === 404,
    });
    if (response.status === 404 || !response.data) {
      return [];
    }
    return [response.data];
  } catch {
    return [];
  }
}
