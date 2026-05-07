import axios from "@/lib/axiosInstance";

export interface TopPoiDto {
  name: string | null;
  typeTag: string;
  address: string | null;
}

export interface ZoneStatsResponse {
  h3Index: string;
  profileId: string;
  populationDensity: number | null;
  estimatedFootTraffic: number | null;
  driverCounts: Record<string, number>;
  competitorCounts: Record<string, number>;
  topPois: TopPoiDto[];
}

export async function fetchZoneStats(h3Index: string, profileId: string): Promise<ZoneStatsResponse> {
  const res = await axios.get<ZoneStatsResponse>("/api/v1/analytics/zone", {
    params: { h3Index, profileId },
  });
  return res.data;
}

