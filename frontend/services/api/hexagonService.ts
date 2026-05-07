import axios from "@/lib/axiosInstance";

export interface HexagonDto {
  h3Index: string;
  /** Present only when `profileId` is passed on the hexagons API (authenticated). */
  score: number | null;
  boundary: { lat: number; lng: number }[];
}

export interface BoundingBox {
  northEast: { lat: number; lng: number };
  southWest: { lat: number; lng: number };
}

export async function fetchHexagonsInBbox(
  bbox: BoundingBox,
  profileId?: string | null,
  h3Resolution: number = 9,
): Promise<HexagonDto[]> {
  const { northEast: ne, southWest: sw } = bbox;
  const bboxParam = `${sw.lng},${sw.lat},${ne.lng},${ne.lat}`;
  const res = Math.min(9, Math.max(7, Math.round(h3Resolution)));
  const response = await axios.get<HexagonDto[]>("/api/v1/hexagons", {
    params: {
      bbox: bboxParam,
      ...(profileId ? { profileId } : {}),
      h3Resolution: res,
    },
  });
  return response.data;
}
