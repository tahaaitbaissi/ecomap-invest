import axios from "@/lib/axiosInstance";
import type { BoundingBox } from "@/services/api/poiService";

export interface HexagonDto {
  h3Index: string;
  score: number;
  boundary: { lat: number; lng: number }[];
}

export async function fetchHexagonsInBbox(bbox: BoundingBox): Promise<HexagonDto[]> {
  const { northEast: ne, southWest: sw } = bbox;
  const bboxParam = `${sw.lng},${sw.lat},${ne.lng},${ne.lat}`;
  const response = await axios.get<HexagonDto[]>(`/api/v1/hexagon?bbox=${bboxParam}`);
  return response.data;
}
