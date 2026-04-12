import axios from "@/lib/axiosInstance";

export interface HexagonDto {
  h3Index: string;
  score: number;
  boundary: { lat: number; lng: number }[];
}

export interface BoundingBox {
  northEast: { lat: number; lng: number };
  southWest: { lat: number; lng: number };
}

export async function fetchHexagonsInBbox(bbox: BoundingBox): Promise<HexagonDto[]> {
  const { northEast: ne, southWest: sw } = bbox;
  const bboxParam = `${sw.lng},${sw.lat},${ne.lng},${ne.lat}`;
  const response = await axios.get<HexagonDto[]>(`/api/v1/hexagons?bbox=${bboxParam}`);
  return response.data;
}
