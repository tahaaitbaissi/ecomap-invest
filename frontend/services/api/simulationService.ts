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
  bbox: string;
}

export interface SimulateResponse {
  affectedHexagons: HexagonDto[];
}

export interface OpportunitySimulateRequest {
  lat: number;
  lng: number;
  profileId: string;
  /** Optional LLM summary; scores stay deterministic on the server. */
  explain: boolean;
}

export interface OpportunitySimulateResponse {
  opportunityScore: number;
  demandScore: number;
  competitionPenalty: number;
  clusterEffectBonus: number;
  businessFitBonus: number;
  h3Index: string;
  competitorCountNearby: number;
  competitorCountInHex: number;
  archetypeId: string;
  explanation: string;
  metrics: Record<string, number>;
}

export async function simulateImpact(body: SimulateRequest): Promise<SimulateResponse> {
  const res = await axios.post<SimulateResponse>("/api/v1/simulate", body);
  return res.data;
}

export async function simulateOpportunity(
  body: OpportunitySimulateRequest,
): Promise<OpportunitySimulateResponse> {
  const res = await axios.post<OpportunitySimulateResponse>("/api/v1/simulate/opportunity", body);
  return res.data;
}

export async function deleteSimulationSession(sessionId: string): Promise<void> {
  await axios.delete(`/api/v1/simulate/${encodeURIComponent(sessionId)}`);
}
