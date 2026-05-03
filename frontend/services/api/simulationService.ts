import axios from "@/lib/axiosInstance";
import type { HexagonDto } from "@/services/api/hexagonService";

export type SimulationImpactType = "DRIVER" | "COMPETITOR";

export interface SimulateRequest {
  lat: number;
  lng: number;
  type: SimulationImpactType;
  tag: string;
  profileId: string;
  sessionId: string;
}

export interface SimulateResponse {
  affectedHexagons: HexagonDto[];
}

export async function simulateImpact(body: SimulateRequest): Promise<SimulateResponse> {
  const res = await axios.post<SimulateResponse>("/api/v1/simulate", body);
  return res.data;
}

export async function deleteSimulationSession(sessionId: string): Promise<void> {
  await axios.delete(`/api/v1/simulate/${encodeURIComponent(sessionId)}`);
}
