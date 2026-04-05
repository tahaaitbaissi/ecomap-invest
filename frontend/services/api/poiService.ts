import axios from "@/lib/axiosInstance";

export interface PoiDto {
  id: string;
  name: string;
  address: string;
  typeTag: string;
  latitude: number;
  longitude: number;
}

export interface BoundingBox {
  northEast: { lat: number; lng: number };
  southWest: { lat: number; lng: number };
}

export async function fetchPoisInBbox(bbox: BoundingBox): Promise<PoiDto[]> {
  const { northEast: ne, southWest: sw } = bbox;
  const bboxParam = `${sw.lng},${sw.lat},${ne.lng},${ne.lat}`;
  const response = await axios.get<PoiDto[]>(`/api/v1/poi?bbox=${bboxParam}`);
  return response.data;
}
