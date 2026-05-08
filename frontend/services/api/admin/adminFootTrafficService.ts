import axios from "@/lib/axiosInstance";

export type DayType = "WD" | "SAT" | "SUN";

export interface AdminFootTrafficParamsResponse {
  archetype: string;
  baseDailyMin: number;
  baseDailyMax: number;
  poiDensityCap: number;
  popDensityCap: number;
  incomeWeight: number;
  hourlyCurveWd: number[];
  hourlyCurveSat: number[];
  hourlyCurveSun: number[];
  seasonalScalers: number[];
  dayScalerSat: number;
  dayScalerSun: number;
  noiseSigma: number;
  updatedAt: string | null;
}

export interface AdminFootTrafficParamsUpsertRequest {
  baseDailyMin: number;
  baseDailyMax: number;
  poiDensityCap: number;
  popDensityCap: number;
  incomeWeight: number;
  hourlyCurveWd: number[];
  hourlyCurveSat: number[];
  hourlyCurveSun: number[];
  seasonalScalers: number[];
  dayScalerSat: number;
  dayScalerSun: number;
  noiseSigma: number;
}

export interface FootTrafficRecomputeApiResponse {
  cellsProcessed: number;
  durationMs: number;
  trafficVersion: number;
}

export interface FootTrafficAuditResponse {
  h3Index: string;
  archetype: string;
  archetypeConfidence: number;
  baselineDaily: number;
  peakHourly: number;
  driverPoiCount: number;
  competitorPoiCount: number;
  transitPoiCount: number;
  popDensity: number;
  avgIncome: number;
  noiseSeed: number;
  computedAt: string | null;
  poiTagCountsSample: Record<string, number>;
  hourlyWeekday: number[];
  hourlySaturday: number[];
  hourlySunday: number[];
  seasonalScalerJune: number;
}

export async function adminListFootTrafficParams(): Promise<AdminFootTrafficParamsResponse[]> {
  const res = await axios.get<AdminFootTrafficParamsResponse[]>("/api/v1/admin/foot-traffic/params");
  return res.data;
}

export async function adminGetFootTrafficParams(archetype: string): Promise<AdminFootTrafficParamsResponse> {
  const res = await axios.get<AdminFootTrafficParamsResponse>(`/api/v1/admin/foot-traffic/params/${archetype}`);
  return res.data;
}

export async function adminUpsertFootTrafficParams(
  archetype: string,
  payload: AdminFootTrafficParamsUpsertRequest,
): Promise<AdminFootTrafficParamsResponse> {
  const res = await axios.put<AdminFootTrafficParamsResponse>(
    `/api/v1/admin/foot-traffic/params/${archetype}`,
    payload,
  );
  return res.data;
}

export async function adminRecomputeFootTraffic(params?: {
  scenarioId?: number;
  h3Prefix?: string;
}): Promise<FootTrafficRecomputeApiResponse> {
  const qs = new URLSearchParams();
  if (params?.scenarioId != null) qs.set("scenarioId", String(params.scenarioId));
  if (params?.h3Prefix) qs.set("h3Prefix", params.h3Prefix);
  const q = qs.toString();
  const res = await axios.post<FootTrafficRecomputeApiResponse>(`/api/v1/admin/foot-traffic/recompute${q ? `?${q}` : ""}`);
  return res.data;
}

export async function adminAuditFootTraffic(h3Index: string): Promise<FootTrafficAuditResponse> {
  const res = await axios.get<FootTrafficAuditResponse>(`/api/v1/admin/foot-traffic/audit/${h3Index}`);
  return res.data;
}

