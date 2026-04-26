import type { BoundingBox } from "@/services/api/poiService";
import { generateMockHexagons } from "@/lib/mockHexagonGenerator";

export interface HexagonDto {
  h3Index: string;
  score: number;
  boundary: { lat: number; lng: number }[];
}

export async function fetchHexagonsInBbox(bbox: BoundingBox): Promise<HexagonDto[]> {
  return generateMockHexagons(bbox);
}
