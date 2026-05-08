import axios from "@/lib/axiosInstance";

export type DayType = "WD" | "SAT" | "SUN";

export interface FootTrafficHourlyApiResponse {
  archetype: string;
  baselineDaily: number;
  peakHourly: number;
  hourly: number[];
}

export async function getFootTrafficHourly(params: {
  h3Index: string;
  day?: DayType;
  month?: number;
}): Promise<FootTrafficHourlyApiResponse> {
  const qs = new URLSearchParams();
  if (params.day) qs.set("day", params.day);
  if (params.month != null) qs.set("month", String(params.month));
  const q = qs.toString();
  const res = await axios.get<FootTrafficHourlyApiResponse>(`/api/v1/foot-traffic/hourly/${params.h3Index}${q ? `?${q}` : ""}`);
  return res.data;
}

