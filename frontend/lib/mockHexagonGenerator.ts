import type { BoundingBox } from "@/services/api/poiService";

interface HexagonDto {
  h3Index: string;
  score: number;
  boundary: { lat: number; lng: number }[];
}

// Pointy-top hexagon vertices around (centerLat, centerLng)
function hexVertices(
  centerLat: number,
  centerLng: number,
  latR: number,
  lngR: number
): { lat: number; lng: number }[] {
  return Array.from({ length: 6 }, (_, i) => {
    const angle = (Math.PI / 3) * i - Math.PI / 6; // pointy-top, first vertex at top
    return {
      lat: centerLat + latR * Math.cos(angle),
      lng: centerLng + lngR * Math.sin(angle),
    };
  });
}

// Deterministic score from grid position — avoids flicker on re-render
function stableScore(row: number, col: number): number {
  const h = Math.sin(row * 127.1 + col * 311.7) * 43758.5453;
  return Math.abs(Math.floor((h - Math.floor(h)) * 101));
}

export function generateMockHexagons(bbox: BoundingBox): HexagonDto[] {
  const { northEast: ne, southWest: sw } = bbox;

  const midLat = (sw.lat + ne.lat) / 2;
  const cosLat = Math.cos((midLat * Math.PI) / 180);

  // ~450 m radius → 0.004° in lat, adjusted for lng compression
  const latR = 0.004;
  const lngR = latR / cosLat;

  // Hex grid spacing (pointy-top)
  const rowStep = latR * 1.5; // vertical distance between row centers
  const colStep = lngR * Math.sqrt(3); // horizontal distance between col centers

  const hexagons: HexagonDto[] = [];

  let row = 0;
  for (let lat = sw.lat - latR; lat <= ne.lat + latR; lat += rowStep, row++) {
    const lngOffset = (row % 2) * (colStep / 2); // odd rows shifted right
    let col = 0;
    for (
      let lng = sw.lng - lngR + lngOffset;
      lng <= ne.lng + lngR;
      lng += colStep, col++
    ) {
      hexagons.push({
        h3Index: `mock_${row}_${col}`,
        score: stableScore(row, col),
        boundary: hexVertices(lat, lng, latR, lngR),
      });
    }
  }

  return hexagons;
}
