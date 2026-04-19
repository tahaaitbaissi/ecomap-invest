import axios from "@/lib/axiosInstance";
import type { GhostMarker } from "@/store/useStore";
import type { HexagonDto } from "@/services/api/hexagonService";

export async function runSimulation(
  sessionId: string,
  markers: GhostMarker[]
): Promise<HexagonDto[]> {
  const res = await axios.post<{ affectedHexagons: HexagonDto[] }>("/api/v1/simulation/run", {
    sessionId,
    markers: markers.map(({ lat, lng, type, tag }) => ({ lat, lng, type, tag })),
  });
  return res.data.affectedHexagons;
}
