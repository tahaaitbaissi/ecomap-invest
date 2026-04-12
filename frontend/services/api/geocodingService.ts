import axios from "@/lib/axiosInstance";

export interface GeocodingResult {
  displayName: string;
  lat: number;
  lng: number;
}

export async function searchAddress(query: string): Promise<GeocodingResult[]> {
  const response = await axios.get<GeocodingResult[]>("/api/v1/geocode", {
    params: { q: query },
  });
  return response.data;
}
